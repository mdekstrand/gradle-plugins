package net.elehack.gradle.science

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths
import java.security.MessageDigest
import java.util.regex.Pattern

class LaTeX extends DefaultTask {
    def master
    List<String> sequence = []
    private def workDir

    /**
     * Specify the master document.  Unless a separate working directory is specified, LaTeX will
     * be run in the directory containing this file.
     *
     * @param doc The master document.  Will be interpreted with {@code project.file(doc)}.
     */
    void master(doc) {
        master = doc
    }

    @InputFile
    File getMasterFile() {
        return project.file(master)
    }

    def getRecordedFiles(String key) {
        def fls = getRelatedFile('fls')
        if (!fls.exists()) return []

        def wantedPat = ~/^$key\s+(?<file>.*)/
        def pwdPat = ~/^PWD\s+(?<dir>.*)/
        def files = []
        def root = workingDir.toPath()
        for (line in fls.readLines()) {
            def m = line =~ pwdPat
            if (m) {
                root = Paths.get(m.group('dir'))
                logger.debug "$fls.name: found working directory {}", root
            } else {
                m = line =~ wantedPat
                if (m) {
                    def path = Paths.get(m.group('file'))
                    if (!path.isAbsolute()) {
                        path = root.resolve(path)
                    }
                    files << path.toFile()
                }
            }
        }
        return files
    }

    @InputFiles
    def getAdditionalInputs() {
        getRecordedFiles('INPUT')
    }

    @OutputFiles
    def getOutputFiles() {
        def outputs = getRecordedFiles('OUTPUT')
        if (outputs.isEmpty()) {
            return [getRelatedFile('pdf'), getRelatedFile('log'), getRelatedFile('fls')]
        } else {
            return outputs
        }
    }

    /**
     * Specify the working directory.
     *
     * @param dir The working directory.
     */
    void workingDir(dir) {
        workDir = dir
    }

    void setWorkingDir(dir) {
        workDir = dir
    }

    File getWorkingDir() {
        if (workDir == null) {
            return project.file(master).parentFile
        } else {
            return project.file(workDir)
        }
    }

    File getRelatedFile(String key) {
        def master = masterFile
        def name = master.name
        def newName = name.replaceAll(/\.\w+$/, ".$key")
        return new File(master.parentFile, newName)
    }

    String getDocumentPath() {
        def master = masterFile
        if (master.parentFile == workingDir) {
            master = master.name - '.tex'
        }
        return master
    }

    @TaskAction
    void buildDocument() {
        if (master == null) {
            throw new IllegalStateException("no master document specified")
        }
        logger.info 'building document {}', master
        logger.debug 'using working directory {}', workingDir
        def results = runLaTeX()

        // Only run BibTeX after initial run
        if (results.needsBibtex()) {
            runBibtex()
        }

        int n = 1
        while (results.needsRerun()) {
            // index positions may have been changed by TeX run, regenerate
            if (results.needsMakeindex()) {
                runMakeindex()
            }
            logger.info 're-running LaTeX'
            results = runLaTeX()
            n += 1
            if (n >= 5) {
                logger.warn 'ran LaTeX 5 times, document may be unstable'
                break
            }
        }
    }

    TeXResults runLaTeX() {
        logger.info 'running {} {}', project.latex.compiler, documentPath
        sequence << 'latex'
        def run = new TeXResults()
        run.addCheckedFile('aux')
        run.addCheckedFile('idx')

        def handler = new TexOutputHandler(project.latex.compiler)
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable project.latex.compiler
            args '-recorder', '-interaction', 'nonstopmode'
            args documentPath
            standardOutput = handler.outputStream
        }

        return run
    }

    void runMakeindex() {
        logger.info 'running {} {}', 'makeindex', documentPath
        sequence << 'makeindex'

        def handler = new ProcessOutputHandler('makeindex')
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable 'makeindex'
            args documentPath
            standardOutput = handler.outputStream
            errorOutput = handler.outputStream
        }
    }

    void runBibtex() {
        logger.info 'running {} {}', 'bibtex', documentPath
        sequence << 'bibtex'

        def handler = new ProcessOutputHandler('bibtex')
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable 'bibtex'
            args documentPath
            standardOutput = handler.outputStream
            errorOutput = handler.outputStream
        }
    }

    private class TeXResults {
        final List<TeXFile> files = []

        TeXFile getFile(String key) {
            files.find { tf -> tf.key == key }
        }

        def addCheckedFile(String key) {
            addCheckedFile(getRelatedFile(key), key)
        }

        def addCheckedFile(File f, String key = null) {
            files << new TeXFile(f, key)
        }

        boolean needsRerun() {
            files.any { tf -> tf.changed() }
        }

        boolean needsMakeindex() {
            def ind = getRelatedFile('ind')
            def texIdx = getFile('idx')
            if (texIdx.file.exists()) {
                return !ind.exists() || texIdx.changed()
            }
            return false
        }

        boolean needsBibtex() {
            def aux = getRelatedFile('aux')
            def bbl = getRelatedFile('bbl')
            if (!aux.exists()) {
                return false
            }
            def bibs = []
            aux.eachLine {
                def m = it =~ /^\\bibdata\{(.+)\}/
                if (m) {
                    bibs << new File(getWorkingDir(), "${m.group(1)}.bib")
                }
            }
            if (!bibs.empty && !bbl.exists()) {
                return true
            } else if (bibs.any({f -> f.lastModified() > bbl.lastModified()})) {
                return true
            } else if (getFile('aux').changed(~/^\\citation\{/)) {
                return true
            } else {
                return false
            }
        }
    }

    private static class TeXFile {
        final String key
        final File file
        final List<String> initialLines

        public TeXFile(File f, String k) {
            file = f
            key = k
            initialLines = f.exists() ? f.readLines() : null
        }

        def List<String> getLines() {
            try {
                file.readLines()
            } catch (FileNotFoundException e) {
                null
            }
        }

        boolean changed() {
            return initialLines != lines
        }

        /**
         * Detect whether the lines matched by the filter have changed. Uses List.grep.
         * @param filter A closure to filter the lines.
         * @return {@code true} if the list of lines maching the filter have changed.
         */
        boolean changed(Object filter) {
            return initialLines.grep(filter) != lines.grep(filter)
        }
    }
}

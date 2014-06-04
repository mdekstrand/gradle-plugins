package net.elehack.gradle.science

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

import java.nio.file.Paths

class LaTeX extends ConventionTask {
    String master
    List<String> sequence = []
    List latexArgs = []
    private def workDir
    String latexCompiler

    /**
     * Specify the master document.  Unless a separate working directory is specified, LaTeX will
     * be run in the directory containing this file.
     *
     * @param doc The master document.  Will be interpreted with {@code project.file(doc)}.
     */
    void master(String doc) {
        master = doc
    }

    @InputFile
    File getMasterFile() {
        def name = master
        if (!(name =~ /\.\w+$/)) {
            name += '.tex'
        }
        return new File(workingDir, name)
    }

    File getLogFile() {
        def name = master - '.tex'
        return new File(workingDir, "${name}.log")
    }

    void latexArgs(Object... args) {
        latexArgs.addAll(args)
    }

    String getDocumentName() {
        def match = master =~ /(.*)\.(tex|ltx)/
        if (match) {
            return match.group(1)
        } else {
            return master
        }
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
        def files = getRecordedFiles('INPUT')
        files.removeAll(outputFiles)
        return files
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
            return project.projectDir
        } else {
            workDir
        }
    }

    File getRelatedFile(String key) {
        def master = masterFile
        def name = master.name
        def newName = name.replaceAll(/\.\w+$/, ".$key")
        return new File(master.parentFile, newName)
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
            results = runLaTeX()
        } else if (results.needsBiber()) {
            runBiber()
            results = runLaTeX()
        }

        int n = 1
        while (!results.failed() && results.needsRerun()) {
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

        printLogMessages()

        if (results.failed()) {
            logger.error 'LaTeX failed with code {}', results.execResult.exitValue
            throw new RuntimeException("failed LaTeX run")
        }
    }

    TeXResults runLaTeX() {
        def compiler = getLatexCompiler()
        logger.info 'running {} {}', compiler, master
        sequence << 'latex'

        def handler = new ProcessOutputHandler(compiler)

        def run = new TeXResults()
        run.addCheckedFile('aux')
        run.addCheckedFile('idx')

        logger.debug 'starting output handler thread'
        handler.start()

        run.execResult = project.exec {
            workingDir = this.workingDir
            executable compiler
            args '-recorder'
            args '-interaction', 'errorstopmode'
            args '-file-line-error'
            args latexArgs
            args documentName
            standardOutput = handler.outputStream
            ignoreExitValue = true
        }
        logger.info 'latex exited with code {}', run.execResult.exitValue

        return run
    }

    void runMakeindex() {
        logger.info 'running {} {}', 'makeindex', master
        sequence << 'makeindex'

        def handler = new ProcessOutputHandler('makeindex')
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable 'makeindex'
            args documentName
            standardOutput = handler.outputStream
            errorOutput = handler.outputStream
        }
    }

    void runBibtex() {
        logger.info 'running {} {}', 'bibtex', master
        sequence << 'bibtex'

        def handler = ProcessOutputHandler.create('bibtex') { line, logger ->
            if (line =~ /^Warning--/) {
                logger.warn line
            } else {
                logger.info line
            }
        }
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable 'bibtex'
            args documentName
            standardOutput = handler.outputStream
        }
    }

    void runBiber() {
        logger.info 'running {} {}', 'biber', master
        sequence << 'biber'

        def handler = ProcessOutputHandler.create('biber') { line, logger ->
            if (line =~ /^WARN -/) {
                logger.warn line
            } else {
                logger.info line
            }
        }
        handler.start()
        project.exec {
            workingDir = this.workingDir
            executable 'biber'
            args documentName
            standardOutput = handler.outputStream
        }
    }

    void printLogMessages() {
        logFile.eachLine { line ->
            if (line =~ /^\.?\/.*:\d+:/) {
                logger.error(line)
            } else if (line =~ /Warning:/) {
                // TODO Track the active input file
                logger.warn(line)
            }
        }
    }

    private class TeXResults {
        final List<TeXFile> files = []
        ExecResult execResult

        TeXFile getFile(String key) {
            files.find { tf -> tf.key == key }
        }

        def addCheckedFile(String key) {
            addCheckedFile(getRelatedFile(key), key)
        }

        def addCheckedFile(File f, String key = null) {
            files << new TeXFile(f, key)
        }

        boolean failed() {
            if (execResult == null) {
                throw new IllegalStateException("TeX not run")
            }
            return execResult.exitValue != 0
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

        boolean needsBiber() {
            def bcfFile = getRelatedFile('bcf')
            def bblFile = getRelatedFile('bbl')
            if (!bcfFile.exists()) {
                return false
            }
            def bcf = new XmlSlurper().parse(bcfFile)
            def bibs = bcf.bibdata.datasource*.text().collect {
                new File(getWorkingDir(), it)
            }
            def lastInputTime = Math.max(bcfFile.lastModified(),
                                         bibs*.lastModified().max())
            if (!bblFile.exists() || bblFile.lastModified() < lastInputTime) {
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

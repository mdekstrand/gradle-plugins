package net.elehack.gradle.science

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths
import java.security.MessageDigest

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
            master = master.name
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

        int n = 1
        while (results.needsRerun()) {
            logger.info 're-running LaTeX'
            results = runLaTeX()
            n += 1
            if (n >= 5) {
                logger.warn 'ran LaTeX 5 times, document may be unstable'
            }
        }
    }

    TeXResults runLaTeX() {
        logger.info 'running {} {}', project.latex.compiler, documentPath
        sequence << 'latex'
        def run = new TeXResults()
        run.addCheckedFile(getRelatedFile('aux'))

        def handler = new TexOutputHandler()
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

    private static class TeXResults {
        final List<TeXFile> files = []

        def addCheckedFile(File f) {
            files << new TeXFile(f)
        }

        boolean needsRerun() {
            files.any { tf -> tf.changed() }
        }
    }

    private static class TeXFile {
        final File file
        final byte[] initialDigest

        public TeXFile(File f) {
            file = f
            initialDigest = digestFile(f)
        }

        def byte[] getFinalDigest() {
            return digestFile(file)
        }

        boolean changed() {
            return !Arrays.equals(initialDigest, finalDigest)
        }
    }

    private static byte[] digestFile(File f) {
        if (!f.exists()) {
            return null
        }
        def digest = MessageDigest.getInstance('MD5')
        digest.update(f.bytes)
        return digest.digest()
    }
}

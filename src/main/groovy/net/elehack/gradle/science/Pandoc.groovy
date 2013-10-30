package net.elehack.gradle.science

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Task for running Pandoc.  Pandoc may be run on multiple documents.
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class Pandoc extends DefaultTask {
    List<Doc> documents = []
    Object outputDir = {
        project.buildDir
    }
    String sourceFormat = 'markdown'
    String outputFormat = 'html'
    List<String> srcFlags = []
    List<String> outFlags = []
    boolean standalone = false
    List<Object> extraArgs = []

    void sourceFormat(String fmt) {
        sourceFormat = fmt
    }

    void outputFormat(String fmt) {
        outputFormat = fmt
    }

    void outputDir(Object obj) {
        outputDir = obj
    }

    void enableExtensions(String... exts) {
        for (ext in exts) {
            srcFlags << "+$ext"
        }
    }
    void disableExtensions(String... exts) {
        for (ext in exts) {
            srcFlags << "-$ext"
        }
    }

    void enableOutputExtensions(String exts) {
        for (ext in exts) {
            outFlags << "+$ext"
        }
    }
    void disableOutputExtensions(String... exts) {
        for (ext in exts) {
            outFlags << "-$ext"
        }
    }

    void standalone(boolean flag) {
        standalone = flag
    }

    /**
     * Add extra arguments to the Pandoc invocation.
     * @param args The extra arguments to add.
     */
    void args(Object... args) {
        extraArgs.addAll(args)
    }

    def getSourceFormatString() {
        def bld = new StringBuilder()
        bld.append(sourceFormat)
        for (flag in srcFlags) {
            bld.append(flag)
        }
        bld.toString()
    }

    def getOutputFormatString() {
        def bld = new StringBuilder()
        bld.append(outputFormat)
        for (flag in outFlags) {
            bld.append(flag)
        }
        bld.toString()
    }

    /**
     * Configure a document to compile with Pandoc.  This takes a single file, and lets
     * you specify per-document options.
     * @param src The source file (interpreted with project.file).
     * @param output The output file name (interpreted with project.file; if null, derived
     * from the source file name).
     */
    void document(Object src, Map options) {
        logger.debug("adding document {}", src)
        def output = options.get('output')
        documents << new Doc(src, output)
    }

    /**
     * Configure some documents to compile with Pandoc.
     * @param args The documents to configure (interpreted with project.files).
     */
    void sources(Object... args) {
        inputs.files(args)
    }

    /**
     * Get the output file for a particular input file.  Looks it up in the documents
     * if specified.
     * @param source
     * @return
     */
    File getOutputFile(File source) {
        Doc doc = documents.find {
            it.inputFile == source
        }
        File out = doc?.outputFile
        if (out == null) {
            out = new File(project.file(outputDir),
                           source.name.replaceAll(/(?<=\.)\w+$/, defaultOutputExtension))
        }
        return out
    }

    @TaskAction
    void runPandoc(IncrementalTaskInputs inputs) {
        inputs.outOfDate { InputFileDetails change ->
            logger.info("compiling {}", change.file)
            project.mkdir(change.file.parentFile)
            project.exec {
                workingDir project.projectDir
                executable project.science.pandoc
                delegate.args '-t', outputFormatString
                delegate.args '-f', sourceFormatString
                if (standalone) {
                    delegate.args '--standalone'
                }
                delegate.args extraArgs
                delegate.args '-o', getOutputFile(change.file)
                delegate.args change.file
            }
        }
    }

    public Pandoc() {
        inputs.files {
            documents.collect {
                it.input
            }
        }
        outputs.files {
            inputs.files.files.collect {
                getOutputFile(it)
            }
        }
    }

    String getDefaultOutputExtension() {
        if (outputFormat == "latex") {
            return 'tex';
        } else if (outputFormat.matches(/html|revealjs|dzslides|s3|slideous/)) {
            return 'html'
        } else {
            throw new RuntimeException("cannot guess extension for format " + outputFormat)
        }
    }

    private class Doc {
        Object input
        Object output

        public Doc(src, dst) {
            input = src
            output = dst
        }

        File getInputFile() {
            project.file(input)
        }

        File getOutputFile() {
            if (output != null) {
                project.file(output)
            } else {
                null
            }
        }
    }
}

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
    List<Doc> documents
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
     * Configure a document to compile with Pandoc.
     * @param src The source file (interpreted with project.file).
     * @param output The output file name (interpreted with project.file; if null, derived
     * from the source file name).
     */
    void document(Object src, Object output = null) {
        documents << new Doc(src, output)
    }

    @TaskAction
    void runPandoc(IncrementalTaskInputs inputs) {
        inputs.outOfDate { InputFileDetails change ->
            def doc = documents.find {
                it.inputFile == change.file
            }
            if (doc == null) {
                logger.warn("cannot find document for {}", change.file)
            } else {
                compileDocument(doc)
            }
        }
    }

    void compileDocument(Doc doc) {
        logger.info("compiling {}", doc.input)
        project.exec {
            workingDir project.projectDir
            executable project.science.pandoc
            delegate.args '-t', outputFormatString
            delegate.args '-f', sourceFormatString
            if (standalone) {
                delegate.args '--standalone'
            }
            delegate.args extraArgs
            delegate.args '-o', doc.outputFile
            delegate.args doc.inputFile
        }
    }

    public Pandoc() {
        inputs.files {
            documents.collect {
                it.input
            }
        }
        outputs.files {
            documents.collect {
                it.outputFile
            }
        }
    }

    private String getDefaultOutputExtension() {
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
                def src = project.file(input)
                def name = src.name
                new File(src.parentFile, name.replaceAll(/(?<=\.)\w+/, '') + defaultOutputExtension)
            }
        }
    }
}

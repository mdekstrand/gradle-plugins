package net.elehack.gradle.science

import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails

/**
 * Task for running Pandoc.  Pandoc may be run on multiple documents.
 *
 * The source files are compiled with Pandoc.  Other files added to {@code inputs.files}
 * are treated as inputs upon which all compiled files depend.  A change to any of them
 * results in recompiling everything.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class Pandoc extends SourceTask {
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
    void document(Map options, Object src) {
        logger.debug("adding document {}", src)
        def output = options.get('output')
        documents << new Doc(src, output)
    }

    /**
     * Get the output file for a particular input file.  Looks it up in the documents
     * if specified.
     * @param source
     * @return
     */
    File getOutputFile(File source) {
        Doc doc = documents.find {
            project.file(it.input) == source
        }
        Object out = doc?.output
        if (out == null) {
            new File(project.file(outputDir),
                     source.name.replaceAll(/(?<=\.)\w+$/, defaultOutputExtension))
        } else {
            project.file(out)
        }
    }

    @TaskAction
    void runPandoc(IncrementalTaskInputs inc) {
        def changed = new HashSet<File>()
        inc.outOfDate { InputFileDetails change ->
            changed << change.file
        }
        def changedNonSource = changed - inputs.sourceFiles.files
        Set<File> toCompile
        if (changedNonSource.isEmpty()) {
            // only source files changed, compile them
            toCompile = changed
        } else {
            // non-source files changed, compile everything
            logger.info("{} non-source files changed", changedNonSource.size())
            for (f in changedNonSource) {
                logger.info("  {}", f)
            }
            toCompile = inputs.sourceFiles.files
        }
        for (source in toCompile) {
            logger.info("compiling {}", source)
            def output = getOutputFile(source)
            project.mkdir(output.parentFile)
            project.exec {
                workingDir project.projectDir
                executable project.science.pandoc
                delegate.args '-t', outputFormatString
                delegate.args '-f', sourceFormatString
                if (standalone) {
                    delegate.args '--standalone'
                }
                delegate.args extraArgs
                delegate.args '-o', output
                delegate.args source
            }
        }
    }

    public Pandoc() {
        inputs.source {
            documents.collect {
                it.input
            }
        }
        outputs.files {
            inputs.sourceFiles.files.collect {
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
    }
}

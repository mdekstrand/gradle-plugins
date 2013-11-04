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
class Pandoc extends SourceTask implements PandocSpec {
    @Delegate PandocSpec spec = new PandocSpecImpl()
    List<Doc> documents = []
    Object outputDir = {
        project.buildDir
    }

    /**
     * Configure a document to compile with Pandoc.  This takes a single file, and lets
     * you specify per-document options.
     * @param src The source file (interpreted with project.file).
     * @param output The output file name (interpreted with project.file; if null, derived
     * from the source file name).
     * @param config A custom configuration closure for this document.
     */
    void document(Map options, Object src, Closure config = null) {
        logger.debug("adding document {}", src)
        def output = options?.get('output')
        documents << new Doc(src, output, config)
    }

    void document(Object src, Closure config) {
        document(null, src, config)
    }

    private Doc lookupDocument(File source) {
        def doc = documents.find {
            project.file(it.input) == source
        }
        return doc ?: new Doc(source, null, null)
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
                     source.name.replaceAll(/(?<=\.)\w+$/, spec.defaultOutputExtension))
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
            def doc = lookupDocument(source)
            PandocSpecImpl spec = copySpec()
            if (doc.config) {
                doc.config.setDelegate(spec)
                doc.config.setResolveStrategy(Closure.DELEGATE_FIRST)
                doc.config.call(source)
            }
            def output
            if (doc.output == null) {
                output = new File(project.file(outputDir),
                                  source.name.replaceAll(/(?<=\.)\w+$/, spec.defaultOutputExtension))
            } else {
                output = project.file(doc.output)
            }
            outputs.file output
            spec.execute(project, source, output)
        }
    }

    public Pandoc() {
        inputs.file { bibliography }
        inputs.file { template }
        inputs.source {
            documents.collect {
                it.input
            }
        }
//        outputs.files {
//            inputs.sourceFiles.files.collect {
//                getOutputFile(it)
//            }
//        }
    }

    private class Doc {
        final Object input
        final Object output
        final Closure config

        public Doc(src, dst, Closure cfg) {
            input = src
            output = dst
            config = cfg;
        }
    }
}

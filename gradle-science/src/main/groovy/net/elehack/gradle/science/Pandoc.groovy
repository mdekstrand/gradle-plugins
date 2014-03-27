package net.elehack.gradle.science

import org.gradle.api.Project
import org.gradle.api.tasks.OutputFiles
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
    @Delegate PandocSpec spec = new PandocSpecImpl(project)
    List<Doc> documents = []
    Object outputDir = {
        project.buildDir
    }

    void outputDir(Object obj) {
        outputDir = obj
    }

    @OutputFiles
    def getOutputFiles() {
        inputs.sourceFiles.files.collect { file ->
            logger.info "looking up document for {}", file
            lookupDocument(file).outputFile
        }
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
        documents << new Doc(this, src, output, config)
    }

    void document(Object src, Closure config) {
        document(null, src, config)
    }

    Doc lookupDocument(File source) {
        def doc = documents.find {
            project.file(it.input) == source
        }
        return doc ?: new Doc(this, source, null, null)
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
            PandocSpecImpl spec = doc.pandocSpec
            def output = doc.getOutputFile(spec)
            spec.execute(source, output)
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
        inputs.property 'args', {
            fullArgs
        }
    }

    private class Doc {
        final Pandoc pandocTask
        final Object input
        final Object output
        final Closure config

        public Doc(Pandoc task, src, dst, Closure cfg) {
            pandocTask = task
            input = src
            output = dst
            config = cfg;
        }

        def getPandocSpec() {
            def spec = copySpec()
            if (config) {
                config.setDelegate(spec)
                config.setResolveStrategy(Closure.DELEGATE_FIRST)
                config.call(input)
            }
            return spec
        }

        def getProject() {
            return pandocTask.project
        }

        File getOutputFile() {
            return getOutputFile(pandocSpec)
        }

        File getOutputFile(PandocSpec spec) {
            if (output == null) {
                def srcName = project.file(input).name
                def outName = srcName.replaceAll(/(?<=\.)\w+$/,
                                                 spec.defaultOutputExtension)
                def outputDir = project.file(pandocTask.outputDir)
                return new File(outputDir, outName)
            } else {
                return project.file(output)
            }
        }
    }
}

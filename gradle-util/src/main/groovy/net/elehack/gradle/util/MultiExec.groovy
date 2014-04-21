package net.elehack.gradle.util

import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Gradle task for applying a program across multiple input files.  It runs the program once for each
 * changed file in its source files.
 */
class MultiExec extends SourceTask {
    /**
     * A closure mapping source files to output files.
     */
    def Closure outputMappingClosure

    def Closure configBlock
    def String method
    def int threadCount = 1

    def getSourceFiles() {
        return source.files
    }

    @OutputFiles
    def getOutputFiles() {
        return sourceFiles.collect { src ->
            getOutput(src)
        }
    }

    /**
     * Specify an output mapping. The closure will be passed a source file, and is expected to
     * return the output file corresponding to that source file.  The return value will be passed
     * to {@link Project#file(Object)}.
     *
     * @param cl
     * @return
     */
    def outputMapping(Closure cl) {
        outputMappingClosure = cl
    }

    /**
     * Get the output file corresponding to an input file.
     * @param input The source file.
     * @return The output file corresponding to {@code input}, as determined by the output mapping.
     */
    File getOutput(File input) {
        return project.file(outputMappingClosure(input))
    }

    /**
     * Specify the program to execute.  The provided block will receive the source and (optionally)
     * output files as arguments, and will be evaluated in the context of {@code project.exec}.
     *
     * @param block The process configuration block.
     */
    void exec(Closure block) {
        if (method != null && method != 'exec') {
            logger.warn('exec called after being configured for {}', method)
        }
        configBlock = block
        method = 'exec'
    }

    /**
     * Specify the Java program to execute.  The provided block will receive the source and (optionally)
     * output files as arguments, and will be evaluated in the context of {@code project.javaexec}.
     * This is mutually exclusive with {@link #exec(Closure)}.
     *
     * @param block The process configuration block.
     */
    void javaexec(Closure block) {
        if (method != null && method != 'javaexec') {
            logger.warn('javaexec called after being configured for {}', method)
        }
        configBlock = block
        method = 'javaexec'
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        def toRebuild = new LinkedHashSet<File>()
        def otherFiles = new HashSet<File>()
        inputs.outOfDate { change ->
            if (sourceFiles.contains(change.file)) {
                logger.info 'queueing file {}', change.file
                toRebuild << change.file
            } else {
                logger.info 'file {} changed, but is not a source file', change.file
                otherFiles << change.file
            }
        }
        if (!otherFiles.isEmpty() && toRebuild.size() != sourceFiles.size()) {
            logger.info '{} non-source files changed, rebuilding all'
            toRebuild = sourceFiles
        }
        def tasks = []
        toRebuild.each { file ->
            tasks << {
                def output = getOutput(file)
                logger.info 'processing {} into {}', file, output
                def block
                if (configBlock.maximumNumberOfParameters > 1) {
                    block = configBlock.curry(file, output)
                } else {
                    block = configBlock.curry(file)
                }
                project.invokeMethod(method, block)
            }
        }
        logger.info 'running {} tasks with method {}', tasks.size(), method
        if (threadCount == 1) {
            logger.info 'running on a single thread'
            for (task in tasks) {
                task.run()
            }
        } else {
            def nt = threadCount
            if (nt <= 0) {
                nt = Runtime.getRuntime().availableProcessors()
            }
            logger.info 'running on {} threads', nt
            ExecutorService svc = Executors.newFixedThreadPool(nt)
            try {
                def results = tasks.collect { t -> svc.submit(t) }
                logger.info 'waiting on results of {} tasks', results.size()
                for (res in results) {
                    try {
                        res.get()
                    } catch (Exception ex) {
                        svc.shutdownNow()
                        throw ex
                    }
                }
            } finally {
                svc.shutdown()
            }
        }
        inputs.removed { change ->
            def output = getOutput(change.file)
            if (output != null) {
                logger.info 'removing {}', output
                project.delete output
            }
        }
    }

}

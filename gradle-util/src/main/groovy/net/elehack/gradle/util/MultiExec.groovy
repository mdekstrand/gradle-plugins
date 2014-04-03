package net.elehack.gradle.util

import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs

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
        inputs.outOfDate { change ->
            logger.info 'processing {}', change.file
            def block
            if (configBlock.maximumNumberOfParameters > 1) {
                block = configBlock.curry(change.file, getOutput(change.file))
            } else {
                block = configBlock.curry(change.file)
            }
            project.invokeMethod(method, block)
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

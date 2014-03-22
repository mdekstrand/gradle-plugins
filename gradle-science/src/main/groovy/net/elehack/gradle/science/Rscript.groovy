package net.elehack.gradle.science

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Task to run an R script.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class Rscript extends ConventionTask {
    String rscriptExecutable
    def script = null
    List<Object> scriptArgs = []
    Object workDir = {
        project.projectDir
    }

    @TaskAction
    void executeRScript() {
        project.exec {
            workingDir project.file(workDir)
            executable getRscriptExecutable()
            args scriptFile
            args scriptArgs
        }
    }

    def script(s) {
        script = s
    }

    def args(Object... args) {
        scriptArgs.addAll(args)
    }

    @InputFile
    File getScriptFile() {
        return project.file(script)
    }
}

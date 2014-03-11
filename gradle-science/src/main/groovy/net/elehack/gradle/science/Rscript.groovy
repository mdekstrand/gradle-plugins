package net.elehack.gradle.science

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * Task to run an R script.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class Rscript extends DefaultTask {
    String rscriptExecutable = null
    def script = null
    List<Object> scriptArgs = []
    Object workDir = {
        project.projectDir
    }

    @TaskAction
    void executeRScript() {
        project.exec {
            workingDir project.file(workDir)
            executable effectiveExecutable
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

    def getEffectiveExecutable() {
        return rscriptExecutable ?: project.extensions.getByName('science')?.rscript
    }
}

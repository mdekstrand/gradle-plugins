package net.elehack.gradle.science

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Plugin to initialize the gradle-science environment.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class SciencePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.logger.info 'setting up science extension'
        project.extensions.create('science', ScienceExtension)
        project.extensions.create('latex', LaTeXExtension)
        if (project.hasProperty('latex.compiler')) {
            project.latex.compiler = project.getProperty('latex.compiler')
        }
        if (project.hasProperty('latex.outputMode')) {
            project.latex {
                outputMode project.getProperty('latex.outputMode')
            }
        }
        if (project.hasProperty('zotero.key')) {
            project.science {
                zoteroKey project.getProperty('zotero.key')
            }
        }
        project.tasks.withType(Rscript.class).all { task ->
            task.conventionMapping.rscriptExecutable = {
                project.extensions.getByName('science')?.rscript ?: 'Rscript'
            }
        }
        project.tasks.withType(LaTeX.class).all { task ->
            logger.info 'configuring task {} for science', task
            task.conventionMapping.outputMode = {
                def ext = project.extensions.getByName('latex')
                ext?.outputMode ?: ErrorOutputMode.DEFAULT
            }
            task.conventionMapping.latexCompiler = {
                def ext = project.extensions.getByName('latex')
                ext?.compiler ?: LaTeXExtension.DEFAULT_COMPILER
            }
        }
        project.tasks.withType(FetchZotero) { task ->
            task.conventionMapping.authKey = {
                project.extensions.getByName('science')?.zoteroKey
            }
        }
    }
}

package net.elehack.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The JRuby plugin.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class JRubyPlugin implements Plugin<Project> {
    static Logger logger = LoggerFactory.getLogger(JRubyPlugin)

    @Override
    void apply(Project project) {
        project.configurations.create('jruby')
        project.extensions.create('jruby', JRubyExtension)
        project.dependencies.add('jruby', "org.jruby:jruby:$project.jruby.version")

        project.task('installBundler') {
            description "Installs bundler to manage rubygems."
            outputs.dir project.jruby.bootstrapPath

            doLast {
                logger.info 'installing Bundler'
                project.javaexec {
                    main 'org.jruby.Main'
                    classpath project.configurations.jruby
                    args '-S', 'gem', 'install'
                    args '-i', project.file(project.jruby.bootstrapPath)
                    if (project.jruby.bundlerVersion != null) {
                        args '-v', project.jruby.bundlerVersion
                    }
                    args 'bundler'
                }
            }
        }

        project.task('installGems', dependsOn: ['installBundler']) {
            description "Installs rubygems with bundler."
            ext.bundler = project.tasks['installBundler']
            inputs.files 'Gemfile', 'Gemfile.lock'
            outputs.dir "$project.jruby.gemRoot/jruby"
            doLast {
                def bindir = project.file("$project.jruby.gemRoot/bin")
                logger.info 'installing Ruby gems with {}', project.jruby.bundlerScript
                project.javaexec {
                    main 'org.jruby.Main'
                    classpath project.configurations.jruby
                    environment.remove('GEM_HOME')
                    environment.remove('JRUBY_HOME')
                    environment 'GEM_PATH', project.file(project.jruby.bootstrapPath)
                    args project.file(project.jruby.bundlerScript)
                    args 'install', "--path=${project.file(project.jruby.gemRoot)}"
                    args "--binstubs=$bindir"
                }
            }
        }

        project.task('cleanJRuby', type: Delete) {
            description "Deletes Ruby packages."
            delete project.jruby.gemRoot
        }
    }
}

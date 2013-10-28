package net.elehack.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.nativecode.cdt.tasks.GenerateMetadataFileTask

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class JRubyPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.configurations.create('jruby')
        project.extensions.create('jruby', JRubyExtension)
        project.dependencies.add('jruby', "org.jruby:jruby:$project.jruby.version")

        project.task('installBundler') {
            description "Installs bundler to manage rubygems."
            outputs.dir project.jruby.bootstrapPath

            doLast {
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
                project.javaexec {
                    main 'org.jruby.Main'
                    classpath project.configurations.jruby
                    environment 'GEM_PATH', project.file(project.jruby.bootstrapPath)
                    args project.file(project.jruby.bundlerScript)
                    args 'install', "--path=${project.file(project.jruby.gemRoot)}"
                }
            }
        }

        project.task('cleanJRuby', type: Delete) {
            description "Deletes Ruby packages."
            delete project.jruby.gemRoot
        }
    }
}

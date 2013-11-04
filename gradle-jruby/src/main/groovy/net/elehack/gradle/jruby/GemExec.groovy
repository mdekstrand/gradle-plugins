package net.elehack.gradle.jruby

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Execute a gem from the installed bundle.  Any task of this type depends
 * on the 'installGems' task, and runs with the <tt>jruby</tt> classpath.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
class GemExec extends DefaultTask {
    String script = null
    List<Object> arguments = []

    void script(String name) {
        script = name
    }

    void args(Object... args) {
        arguments.addAll(args)
    }

    @TaskAction
    void runBundleScript() {
        logger.info("running bundler script {}", script)
        project.javaexec {
            main 'org.jruby.Main'
            classpath project.configurations.jruby
            args project.file("$project.jruby.gemRoot/bin/$script")
            args arguments
            environment.remove('GEM_HOME')
            environment.remove('JRUBY_HOME')
            environment 'GEM_PATH', project.file(project.jruby.bootstrapPath)
        }
    }

    GemExec() {
        dependsOn 'installGems'
    }
}

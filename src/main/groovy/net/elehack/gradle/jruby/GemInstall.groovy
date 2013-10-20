package net.elehack.gradle.jruby

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.jruby.CompatVersion
import org.jruby.embed.LocalContextScope
import org.jruby.embed.ScriptingContainer

/**
 * Task to install Ruby gems.
 *
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class GemInstall extends DefaultTask {
    String getGemRoot() {
        return project.file(project.jruby.gemRoot).absolutePath
    }

    List<RubyGem> getGems() {
        return project.jruby.gems
    }

    @TaskAction
    void installGems() {
        project.logger.info("installing {} gems to {}", gems.size(), gemRoot)
        for (gem in gems) {
            // TODO Check if the gem is already installed
            project.logger.info("installing gem {}", gem.name)
            def args = ['install', '-i', gemRoot]
            if (gem.version != null) {
                args << '-v'
                args << gem.version
            }
            args << gem.name
            def ruby = new ScriptingContainer(LocalContextScope.THREADSAFE)
            ruby.compatVersion = CompatVersion.RUBY1_9
            ruby.argv = args.toArray(new String[args.size()])
            GemInstall.getResourceAsStream("gem.rb").with { stream ->
                ruby.runScriptlet(stream, "gem.rb")
            }
        }
    }
}

package net.elehack.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class JRubyPlugin implements Plugin<Project> {
    @Override
    void apply(Project t) {
        t.extensions.create("jruby", JRubyExtension)
        t.task('installGems', type: GemInstall)
    }
}

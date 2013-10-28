package net.elehack.gradle.jruby

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class JRubyExtension {
    String version = '1.7.6'
    String bundlerVersion = null

    Object gemRoot = 'ruby'

    def bootstrapPath = {
        "$gemRoot/bootstrap"
    }

    def getBundlerScript() {
        def path = bootstrapPath instanceof Closure ? bootstrapPath() : bootstrapPath
        return "$path/bin/bundle"
    }
}
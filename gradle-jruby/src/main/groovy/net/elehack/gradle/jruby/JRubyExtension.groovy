package net.elehack.gradle.jruby

/**
 * Extension object for the JRuby plugin.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
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
package net.elehack.gradle.jruby

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
class JRubyExtension {
    def gemRoot = ".jruby"
    def List<RubyGem> gems = new ArrayList<>()

    def gem(String name) {
        gems << new RubyGem(name)
    }
    def gem(String name, String version) {
        gems << new RubyGem(name, version);
    }
}

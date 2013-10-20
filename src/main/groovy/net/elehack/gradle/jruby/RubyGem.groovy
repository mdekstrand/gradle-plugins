package net.elehack.gradle.jruby

/**
 * @author <a href="http://www.grouplens.org">GroupLens Research</a>
 */
private class RubyGem {
    final String name
    final String version

    RubyGem(String n) {
        this(n, null)
    }
    RubyGem(String n, String v) {
        name = n;
        version = v;
    }
}

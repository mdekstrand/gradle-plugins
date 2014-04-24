package net.elehack.gradle.science;

/**
 * Extension for configuring the gradle-science tasks.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
public class ScienceExtension {
    /**
     * The Pandoc executable.
     */
    String pandoc = 'pandoc'

    /**
     * The Rscript executable.
     */
    String rscript = 'Rscript'

    /**
     * A Zotero authentication key.
     */
    String zoteroKey
}

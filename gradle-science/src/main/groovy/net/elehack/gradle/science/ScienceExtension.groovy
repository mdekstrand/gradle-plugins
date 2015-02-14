package net.elehack.gradle.science;

import org.gradle.api.Project

/**
 * Extension for configuring the gradle-science tasks.
 *
 * @author <a href="http://elehack.net">Michael Ekstrand</a>
 */
public class ScienceExtension {
    /**
     * The Pandoc executable.
     */
    String pandoc

    /**
     * The Rscript executable.
     */
    String rscript

    /**
     * A Zotero authentication key.
     */
    String zoteroKey

    ScienceExtension(Project prj) {
        def prop = { String name, String dft ->
            prj.hasProperty("science.$name") ? prj.getProperty("science.$name") : dft
        }

        pandoc = prop('pandoc', 'pandoc')
        rscript = prop('rscript', 'Rscript')
    }
}

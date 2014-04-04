package net.elehack.gradle.util

import org.gradle.api.Project
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.signatory.Signatory
import org.gradle.plugins.signing.signatory.SignatoryProvider

/**
 * Signatory provider to use the GnuPG command-line tool.  Assign an instance of this to the
 * {@code signing.signatories} property.
 */
class GnuPGSignatoryProvider implements SignatoryProvider {
    private Map<String,GnuPGSignatory> signatories = new HashMap()
    @Override
    void configure(SigningExtension signingExtension, Closure closure) {
        /* no-op - we do not support configuration. */
    }

    @Override
    Signatory getDefaultSignatory(Project project) {
        if (project.hasProperty('signing.keyId')) {
            return new GnuPGSignatory(project, project.getProperty('signing.keyId'))
        } else {
            return null
        }
    }

    @Override
    Signatory getSignatory(String s) {
        return signatories[s]
    }
}

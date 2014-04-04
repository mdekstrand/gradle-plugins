package net.elehack.gradle.util

import org.gradle.api.Project
import org.gradle.plugins.signing.signatory.SignatorySupport
import org.gradle.plugins.signing.signatory.pgp.PgpKeyId

/**
 * Signatory using GnuPG command-line tool.
 */
class GnuPGSignatory extends SignatorySupport {
    private Project project

    public GnuPGSignatory(Project prj) {
        project = prj
    }

    @Override
    String getName() {
        return "GnuPG"
    }

    @Override
    void sign(InputStream inputStream, OutputStream outputStream) {
        project.exec {
            executable 'gpg'
            args '--detach-sign'
            if (keyId != null) {
                args '--local-user', keyId.asHex
            }
            standardInput = inputStream
            standardOutput = outputStream
        }
    }

    PgpKeyId getKeyId() {
        if (project.hasProperty('signing.keyId')) {
            return new PgpKeyId(project.getProperty('signing.keyId') as String)
        } else {
            return null
        }
    }
}

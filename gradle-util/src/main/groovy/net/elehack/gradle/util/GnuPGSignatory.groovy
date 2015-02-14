package net.elehack.gradle.util

import org.gradle.api.Project
import org.gradle.plugins.signing.signatory.SignatorySupport
import org.gradle.plugins.signing.signatory.pgp.PgpKeyId
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Signatory using GnuPG command-line tool.
 */
class GnuPGSignatory extends SignatorySupport {
    private Logger logger = LoggerFactory.getLogger(GnuPGSignatory)
    private Project project
    private PgpKeyId keyId

    public GnuPGSignatory(Project prj, String key) {
        project = prj
        keyId = new PgpKeyId(key)
    }

    @Override
    String getName() {
        return "GnuPG"
    }

    @Override
    void sign(InputStream inputStream, OutputStream outputStream) {
        logger.info("signing with key {}", keyId.asHex)
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
        return keyId;
    }
}

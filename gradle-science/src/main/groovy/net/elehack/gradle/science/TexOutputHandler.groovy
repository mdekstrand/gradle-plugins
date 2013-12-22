package net.elehack.gradle.science

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TexOutputHandler extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(TexOutputHandler)
    private PipedOutputStream output
    private PipedInputStream input
    private BufferedReader reader

    public TexOutputHandler() {
        input = new PipedInputStream()
        output = new PipedOutputStream(input)
        reader = new BufferedReader(new InputStreamReader(input))
    }

    public OutputStream getOutputStream() {
        return output;
    }

    @Override
    void run() {
        logger.debug "Starting TeX output handling thread"
        def errorPattern = ~/Error:/
        def warningPattern = ~/Warning:/
        def errLinePattern = ~/^l\.\d+/
        try {
            String line
            boolean inError = false
            while ((line = reader.readLine()) != null) {
                if (inError) {
                    logger.error line
                    if (line =~ errLinePattern) {
                        inError = false
                    }
                } else if (line =~ errorPattern) {
                    logger.error line
                    inError = true
                } else if (line =~ warningPattern) {
                    logger.warn line
                } else {
                    logger.info line
                }
            }
            logger.debug "TeX output thread shutting down"
        } finally {
            reader.close()
        }
    }
}

package net.elehack.gradle.science

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Handler for process output.  This implementation just writes output to the logger at the
 * info level.
 */
class ProcessOutputHandler extends Thread {
    protected Logger logger
    private PipedOutputStream output
    private PipedInputStream input
    private BufferedReader reader

    /**
     * Construct a new logger.
     *
     * @param name A name to identify the process (used to construct the logger name).
     */
    public ProcessOutputHandler(String name) {
        logger = LoggerFactory.getLogger("process.$name")
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
            while ((line = reader.readLine()) != null) {
                handleLine line
            }
            logger.debug "TeX output thread shutting down"
        } finally {
            reader.close()
        }
    }

    protected void handleLine(String line) {
        logger.info line
    }
}

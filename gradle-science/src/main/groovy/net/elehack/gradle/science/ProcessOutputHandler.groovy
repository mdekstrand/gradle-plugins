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

    /**
     * Construct a new logger.
     *
     * @param name A name to identify the process (used to construct the logger name).
     * @param handle Closure that handles each line.
     */
    public static ProcessOutputHandler create(String name, Closure lh) {
        return new ConfigurableOutputHandler(name, lh)
    }

    public OutputStream getOutputStream() {
        return output;
    }

    @Override
    void run() {
        logger.debug "Starting process output handling thread"
        try {
            String line
            while ((line = reader.readLine()) != null) {
                handleLine line
            }
            logger.debug "Process output thread shutting down"
        } finally {
            reader.close()
        }
    }

    protected void handleLine(String line) {
        logger.info line
    }
}

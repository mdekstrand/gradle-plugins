package net.elehack.gradle.science

import org.slf4j.Logger

/**
 * TeX process output handler.  Parses TeX output for errors/warnings, and defers them until after
 * the process has finished so we only print for the last run.
 */
class TexOutputHandler extends ProcessOutputHandler {
    private boolean inError = false
    private def errorPattern = ~/Error:/
    private def warningPattern = ~/Warning:/
    private def errLinePattern = ~/^l\.\d+/
    private List<Message> messages = new ArrayList<>()

    public TexOutputHandler(String name) {
        super(name)
    }

    @Override
    protected void handleLine(String line) {
        if (inError) {
            messages << new Message(Level.ERROR, line)
            if (line =~ errLinePattern) {
                inError = false
            }
        } else if (line =~ errorPattern) {
            messages << new Message(Level.ERROR, line)
            inError = true
        } else if (line =~ warningPattern) {
            messages << new Message(Level.WARN, line)
        }

        logger.info line
    }

    public void printMessages() {
        for (msg in messages) {
            switch (msg.level) {
                case Level.WARN:
                    logger.warn msg.content
                    break;
                case Level.ERROR:
                    logger.error msg.content
                    break;
            }
        }
    }

    private static enum Level {
        WARN, ERROR
    }

    private static class Message {
        final Level level
        final String content

        Message(Level lvl, String str) {
            level = lvl
            content = str
        }
    }
}

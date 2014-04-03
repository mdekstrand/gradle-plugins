package net.elehack.gradle.science

import org.slf4j.Logger

/**
 * TeX process output handler.  Parses TeX output for errors/warnings, and defers them until after
 * the process has finished so we only print for the last run.
 */
class TexOutputHandler extends ProcessOutputHandler {
    private boolean inError = false
    private final def errorPattern = ~/^!/
    private final def warningPattern = ~/Warning:/
    private final def errLinePattern = ~/^l\.\d+/
    private ErrorOutputMode outputMode
    private List<Message> messages = new ArrayList<>()

    public TexOutputHandler(String name, ErrorOutputMode mode) {
        super(name)
        outputMode = mode ?: ErrorOutputMode.DEFAULT
    }

    @Override
    protected void handleLine(String line) {
        def level = MessageLevel.INFO
        if (inError) {
            if (line =~ errLinePattern) {
                inError = false
            }
            if (outputMode == ErrorOutputMode.IMMEDIATE) {
                level = MessageLevel.ERROR
            } else {
                messages << new Message(MessageLevel.ERROR, line)
            }
        } else if (line =~ errorPattern) {
            inError = true
            if (outputMode == ErrorOutputMode.IMMEDIATE) {
                level = MessageLevel.ERROR
            } else {
                messages << new Message(MessageLevel.ERROR, line)
            }
        } else if (line =~ warningPattern) {
            if (outputMode == ErrorOutputMode.IMMEDIATE) {
                level = MessageLevel.WARN
            } else {
                messages << new Message(MessageLevel.WARN, line)
            }
        }

        level.print(logger, line)
    }

    public void printMessages() {
        for (msg in messages) {
            msg.level.print(logger, msg.content)
        }
    }

    private static class Message {
        final MessageLevel level
        final String content

        Message(MessageLevel lvl, String str) {
            level = lvl
            content = str
        }
    }
}

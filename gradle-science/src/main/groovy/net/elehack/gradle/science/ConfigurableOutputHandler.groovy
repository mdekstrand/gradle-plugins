package net.elehack.gradle.science

class ConfigurableOutputHandler extends ProcessOutputHandler {
    private Closure lineHandler

    ConfigurableOutputHandler(String name, Closure lh) {
        super(name)
        lineHandler = lh
    }

    @Override
    protected void handleLine(String line) {
        lineHandler(line, logger)
    }
}

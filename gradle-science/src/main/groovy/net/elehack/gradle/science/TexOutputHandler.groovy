package net.elehack.gradle.science
/**
 * TeX process output handler.
 */
class TexOutputHandler extends ProcessOutputHandler {
    private boolean inError = false
    private def errorPattern = ~/Error:/
    private def warningPattern = ~/Warning:/
    private def errLinePattern = ~/^l\.\d+/

    public TexOutputHandler(String name) {
        super(name)
    }

    @Override
    protected void handleLine(String line) {
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
}

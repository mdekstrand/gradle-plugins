package net.elehack.gradle.science

class LaTeXExtension {
    static String DEFAULT_COMPILER = 'pdflatex'
    /**
     * The program to use to compile LaTeX.
     */
    String compiler = DEFAULT_COMPILER

    ErrorOutputMode outputMode = ErrorOutputMode.DEFAULT

    /**
     * Set the LaTeX compiler.
     * @param comp
     * @return
     */
    def compiler(String comp) {
        compiler = comp
    }

    def outputMode(ErrorOutputMode mode) {
        outputMode = mode
    }

    def outputMode(String mode) {
        outputMode = ErrorOutputMode.valueOf(mode.toUpperCase())
    }
}

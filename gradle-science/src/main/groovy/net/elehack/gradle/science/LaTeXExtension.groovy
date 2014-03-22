package net.elehack.gradle.science

class LaTeXExtension {
    static String DEFAULT_COMPILER = 'pdflatex'
    /**
     * The program to use to compile LaTeX.
     */
    String compiler = DEFAULT_COMPILER

    /**
     * Set the LaTeX compiler.
     * @param comp
     * @return
     */
    def compiler(String comp) {
        compiler = comp
    }
}

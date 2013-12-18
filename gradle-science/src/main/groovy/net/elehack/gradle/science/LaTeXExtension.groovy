package net.elehack.gradle.science

class LaTeXExtension {
    /**
     * The program to use to compile LaTeX.
     */
    String compiler = 'pdflatex'

    /**
     * Set the LaTeX compiler.
     * @param comp
     * @return
     */
    def compiler(String comp) {
        compiler = comp
    }
}

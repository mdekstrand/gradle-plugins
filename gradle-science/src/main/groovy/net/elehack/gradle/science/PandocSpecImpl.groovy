package net.elehack.gradle.science

import org.gradle.api.Project

/**
 * Specify a Pandoc execution.
 */
class PandocSpecImpl implements PandocSpec {
    String sourceFormat = 'markdown'
    String outputFormat = 'html'
    List<String> srcFlags = []
    List<String> outFlags = []
    boolean standalone = false
    List<Object> extraArgs = []
    def bibliography = null
    def citationStyle = null
    String filter = null
    def template = null

    void sourceFormat(String fmt) {
        sourceFormat = fmt
    }

    void outputFormat(String fmt) {
        outputFormat = fmt
    }

    void enableExtensions(String... exts) {
        for (ext in exts) {
            srcFlags << "+$ext"
        }
    }
    void disableExtensions(String... exts) {
        for (ext in exts) {
            srcFlags << "-$ext"
        }
    }

    void enableOutputExtensions(String exts) {
        for (ext in exts) {
            outFlags << "+$ext"
        }
    }
    void disableOutputExtensions(String... exts) {
        for (ext in exts) {
            outFlags << "-$ext"
        }
    }

    void standalone(boolean flag) {
        standalone = flag
    }

    void template(Object tmpl) {
        template = tmpl;
    }

    /**
     * Add extra arguments to the Pandoc invocation.
     * @param args The extra arguments to add.
     */
    void args(Object... args) {
        extraArgs.addAll(args)
    }

    void bibliography(Object bib) {
        bibliography = bib
    }

    void citationStyle(Object csl) {
        citationStyle = csl
    }

    void filter(String f) {
        filter = f;
    }

    String getSourceFormatString() {
        def bld = new StringBuilder()
        bld.append(sourceFormat)
        for (flag in srcFlags) {
            bld.append(flag)
        }
        bld.toString()
    }

    String getOutputFormatString() {
        def bld = new StringBuilder()
        bld.append(outputFormat)
        for (flag in outFlags) {
            bld.append(flag)
        }
        bld.toString()
    }

    String getDefaultOutputExtension() {
        if (outputFormat == "latex") {
            return 'tex';
        } else if (outputFormat.matches(/html|revealjs|dzslides|s3|slideous/)) {
            return 'html'
        } else {
            throw new RuntimeException("cannot guess extension for format " + outputFormat)
        }
    }

    PandocSpecImpl copySpec() {
        def spec = new PandocSpecImpl()
        for (prop in spec.metaClass.properties) {
            if (prop.name == 'metaClass' || prop.name == 'class') {
                continue;
            }
            def val = prop.getProperty(this)
            if (val instanceof List) {
                val = new ArrayList(val)
            }
            try {
                prop.setProperty(spec, val)
            } catch (GroovyRuntimeException ex) {
                /* no-op */
            }
        }
        spec
    }

    void execute(Project project, File input, File output) {
        project.mkdir(output.parentFile)
        project.exec {
            workingDir project.projectDir
            executable project.science.pandoc
            args '-t', outputFormatString
            args '-f', sourceFormatString
            if (standalone) {
                args '--standalone'
            }
            if (template != null) {
                args '--template', project.file(template)
            }
            if (bibliography != null) {
                args '--bibliography', project.file(bibliography)
            }
            if (citationStyle != null) {
                args '--csl', project.file(citationStyle)
            }
            if (filter != null) {
                args '--filter', filter
            }
            args extraArgs
            args '-o', output
            args input
        }
    }
}

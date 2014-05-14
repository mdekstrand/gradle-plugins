package net.elehack.gradle.science

import org.gradle.api.*
import org.gradle.api.tasks.*

class LaTeXTags extends SourceTask {
    def output = 'tags'

    @OutputFile
    File getOutputFile() {
        return project.file(output);
    }

    @TaskAction
    void makeTags() {
        def lblPat = ~/\\(label\{([^}]+)\})/
        def root = project.projectDir.toPath()
        outputFile.withPrintWriter { out ->
            for (file in source) {
                def relname = root.relativize(file.toPath()).toString()
                logger.info 'scanning {} for tags', relname
                file.eachLine { line, n ->
                    def m = lblPat.matcher(line)
                    if (m.find()) {
                        def tag = m.group(2)
                        def search = m.group(1)
                        logger.info '{}:{}: {}', relname, n, search
                        out.println "$tag\t$relname\t/$search/"
                    }
                }
            }
        }
    }
}

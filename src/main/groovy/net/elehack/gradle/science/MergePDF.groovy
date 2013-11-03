package net.elehack.gradle.science

import com.itextpdf.text.pdf.PdfAction
import com.itextpdf.text.pdf.PdfConcatenate
import com.itextpdf.text.pdf.PdfOutline
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.SimpleBookmark
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class MergePDF extends DefaultTask {
    List<PdfMember> members = new LinkedList<>()

    Object output

    @InputFiles
    List<File> getInputFiles() {
        return members.collect {
            it.sourceFile
        }
    }

    void member(Map opts, src) {
        members << new PdfMember(project, src, opts)
    }

    void output(Object out) {
        output = out
    }

    @OutputFile
    File getOutputFile() {
        project.file(output)
    }

    @TaskAction
    void mergePdfs() {
        outputFile.withOutputStream { out ->
            def cat = new PdfConcatenate(out)
            def outline = []
            int page = 1
            for (mem in members) {
                logger.info "merging file {} on page {}", mem.source, page
                def bookmark = [
                        Title: mem.title,
                        Action: 'GoTo',
                        Page: "$page XYZ 0 10000 0".toString()
                ]
                outline << bookmark
                mem.sourceFile.withInputStream { data ->
                    def reader = new PdfReader(data)
                    cat.addPages(reader)
                    page += reader.numberOfPages
                    def inputbms = SimpleBookmark.getBookmark(reader)
                    SimpleBookmark.shiftPageNumbers(inputbms, page - 1, null)
                    // bookmark['Kids'] = inputbms
                    reader.close()
                }
            }
            cat.writer.setOutlines(outline)
            cat.close()
        }
    }

    private class PdfMember {
        private final Project project
        public final Object source
        public final Map options

        public PdfMember(Project prj, Object src, Map opts) {
            project = prj
            source = src
            options = opts
        }

        public File getSourceFile() {
            return project.file(source)
        }

        public String getTitle() {
            return options.get('title') ?: sourceFile.name
        }
    }
}

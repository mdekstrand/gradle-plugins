package net.elehack.gradle.science

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

class LaTeX extends DefaultTask {
    def master
    private def workDir

    /**
     * Specify the master document.  Unless a separate working directory is specified, LaTeX will
     * be run in the directory containing this file.
     *
     * @param doc The master document.  Will be interpreted with {@code project.file(doc)}.
     */
    void master(doc) {
        master = doc
    }

    @InputFile
    File getMasterFile() {
        return project.file(master)
    }

    /**
     * Specify the working directory.
     *
     * @param dir The working directory.
     */
    void workingDir(dir) {
        workDir = dir
    }

    void setWorkingDir(dir) {
        workDir = dir
    }

    File getWorkingDir() {
        if (workDir == null) {
            return project.file(master).parentFile
        } else {
            return project.file(workDir)
        }
    }

    File getRelatedFile(String key) {
        def master = masterFile
        def name = master.name
        def newName = name.replaceAll(/\.\w+$/, ".$key")
        return new File(master.parentFile, newName)
    }

    @TaskAction
    void runLatex() {
        if (master == null) {
            throw new IllegalStateException("no master document specified")
        }
        def task = this
        def master = masterFile
        if (master.parentFile == workingDir) {
            master = master.name
        }
        project.exec {
            workingDir = task.workingDir
            executable project.latex.compiler
            args '-recorder', '-interaction=errorstopmode'
            args master
        }
    }
}

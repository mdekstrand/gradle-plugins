package net.elehack.gradle.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GitClone extends DefaultTask {
    private def dir
    private def src
    boolean bare = false

    void setDirectory(d) {
        dir = d
    }

    File getDirectory() {
        return project.file(dir)
    }

    void directory(d) {
        dir = d
    }

    void setRemote(r) {
        src = r
    }

    String getRemote() {
        return src
    }

    void remote(r) {
        src = r
    }

    GitExtension getGitExt() {
        return project.extensions.getByType(GitExtension)
    }

    @TaskAction
    void doClone() {
        def dir = directory
        if (dir.exists()) {
            logger.info '{} already exists', dir
            def frb = new FileRepositoryBuilder()
            if (bare) {
                frb.gitDir = directory
            } else {
                frb.gitDir = new File(directory, '.git')
            }
            def repo = frb.build()
            if (repo.workTree != directory && repo.directory != directory) {
                logger.error '{} is not a Git repository', directory
                throw new RuntimeException("cannot find Git repo at $directory")
            }
            def git = new Git(repo)
            logger.info 'pulling into {}'
            gitExt.prepCommand(git.pull()
                                  .setRemote("origin"))
                  .call()
        } else {
            logger.info 'cloning new repository'
            def cmd = Git.cloneRepository()
            cmd.directory = dir
            cmd.bare = bare
            cmd.URI = remote
            gitExt.prepCommand(cmd).call()
        }
    }
}

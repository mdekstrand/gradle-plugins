package net.elehack.gradle.git

import org.eclipse.jgit.api.Git
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class GitPush extends DefaultTask {
    private def repo
    private def remote
    private def refs = []

    void setRepository(d) {
        repo = d
    }

    File getRepository() {
        return project.file(repo)
    }

    void repository(d) {
        repo = d
    }

    void refs(String... specs) {
        for (spec in specs) {
            refs << spec
        }
    }

    GitExtension getGitExt() {
        return project.extensions.getByType(GitExtension)
    }

    @TaskAction
    void doPush() {
        def repo = gitExt.repo(repository)
        def git = new Git(repo)
        def cmd = git.push()
        cmd.remote = remote
        cmd.refSpecs = new ArrayList(refs)
        cmd.call()
    }
}

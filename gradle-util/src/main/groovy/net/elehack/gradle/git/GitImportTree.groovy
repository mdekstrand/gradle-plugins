package net.elehack.gradle.git

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCacheBuilder
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.PersonIdent
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.WorkResult
import org.slf4j.LoggerFactory

/**
 * Import a (sub) tree into a Git repository.
 */
public class GitImportTree extends AbstractCopyTask {
    def repo
    String message
    PersonIdent author

    void setRepository(d) {
        repo = d
    }

    File getRepository() {
        return project.file(repo)
    }

    void repository(d) {
        repo = d
    }

    void author(String name, String email) {
        author = new PersonIdent(name, email)
    }

    GitExtension getGitExt() {
        return project.extensions.getByType(GitExtension)
    }

    @Override
    protected CopyAction createCopyAction() {
        return new GitTreeCopyAction()
    }

    private class GitTreeCopyAction implements CopyAction {
        private final def logger = LoggerFactory.getLogger(getClass())
        @Override
        WorkResult execute(CopyActionProcessingStream stream) {
            def repo = getGitExt().repo(getRepository())
            def index = null
            def inserter = null
            def git = null
            try {
                index = repo.lockDirCache()
                def builder = index.builder()

                def prefix = getRootSpec().destPath.pathString
                for (int i = 0; i < index.getEntryCount(); i++) {
                    def e = index.getEntry(i)
                    if (!e.pathString.startsWith(prefix)) {
                        builder.keep(i, 1)
                    }
                }

                inserter = repo.newObjectInserter()
                logger.info 'walking tree'
                stream.process(new GitTreeStreamAction(inserter, builder))
                inserter.flush()
                builder.commit()
                index = null

                logger.info 'committing changes'
                git = new Git(repo)
                getGitExt().prepCommand(git.commit()
                                           .setAuthor(getAuthor())
                                           .setMessage(getMessage()))
                           .call()
            } finally {
                if (inserter != null) {
                    inserter.release()
                }
                if (index != null) {
                    index.unlock()
                }
                if (git != null) {
                    git.close()
                }
                repo.close()
            }
            return new SimpleWorkResult(true);
        }
    }

    private class GitTreeStreamAction implements CopyActionProcessingStreamAction {
        private final def logger = LoggerFactory.getLogger(getClass())

        ObjectInserter inserter
        DirCacheBuilder builder

        GitTreeStreamAction(ObjectInserter ins, DirCacheBuilder bld) {
            inserter = ins
            builder = bld
        }

        @Override
        void processFile(FileCopyDetailsInternal file) {
            def entry = new DirCacheEntry(file.relativePath.pathString.getBytes('UTF-8'))
            if (file.isDirectory()) {
                return
            }

            logger.info 'adding file {}', entry.pathString
            entry.length = file.size
            entry.lastModified = file.lastModified
            if ((file.mode & 0111) != 0) {
                entry.fileMode = FileMode.EXECUTABLE_FILE
            } else {
                entry.fileMode = FileMode.REGULAR_FILE
            }
            def stream = file.open()
            try {
                entry.objectId = inserter.insert(Constants.OBJ_BLOB,
                                                 file.size, stream)
            } finally {
                stream.close()
            }
            builder.add(entry)
        }
    }
}

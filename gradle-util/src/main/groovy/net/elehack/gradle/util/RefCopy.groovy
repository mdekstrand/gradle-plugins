package net.elehack.gradle.util

import org.gradle.api.Project
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.DefaultFileCopyDetails
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.internal.tasks.SimpleWorkResult
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.WorkResult

public class RefCopy extends Copy {
    @Override
    AbstractCopyTask filter(Map<String, ?> properties, Class<? extends FilterReader> filterType) {
        throw new UnsupportedOperationException("Filtering not supported")
    }

    @Override
    AbstractCopyTask filter(Class<? extends FilterReader> filterType) {
        throw new UnsupportedOperationException("Filtering not supported")
    }

    @Override
    AbstractCopyTask filter(Closure closure) {
        throw new UnsupportedOperationException("Filtering not supported")
    }

    @Override
    protected CopyAction createCopyAction() {
        return new RefCopyAction(project, fileLookup.getFileResolver(destinationDir))
    }

    private class RefCopyAction implements CopyAction {
        private final Project project
        private final FileResolver resolver

        public RefCopyAction(Project prj, FileResolver fr) {
            project = prj
            resolver = fr
        }

        @Override
        public WorkResult execute(CopyActionProcessingStream stream) {
            InternalAction action = new InternalAction(project, resolver)
            stream.process(action)
            return new SimpleWorkResult(action.didWork)
        }
    }

    private class InternalAction implements CopyActionProcessingStreamAction {
        private final Project project
        private final FileResolver resolver
        private boolean didWork = false

        public InternalAction(Project prj, FileResolver fr) {
            project = prj
            resolver = fr
        }

        @Override
        public void processFile(FileCopyDetailsInternal details) {
            File target = resolver.resolve(details.relativePath.pathString)
            if (!details.isDirectory()) {
                // copy the file as-is
                logger.info('copying file {} with reflink', details.relativePath)
                // FIXME Don't copy if it's already up-to-date
                // FIXME Work on Mac & Windows
                project.exec {
                    executable 'cp'
                    args '--reflink=auto', '-p'
                    args details.file, target
                }
                didWork = true
            } else {
                didWork |= details.copyTo(target)
            }
        }
    }
}

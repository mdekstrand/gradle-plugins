package net.elehack.gradle.science;

import org.gradle.api.internal.file.BaseDirFileResolver;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.api.tasks.WorkResult;
import org.gradle.internal.nativeplatform.filesystem.FileSystems;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LinkTree extends Copy {
    private static final Logger logger = LoggerFactory.getLogger(LinkTree.class);

    protected CopyAction createCopyAction() {
        File destDir = getDestinationDir();
        destDir.mkdirs();
        FileResolver resolver = new BaseDirFileResolver(FileSystems.getDefault(), destDir);
        return new LinkAction(resolver);
    }

    private class LinkAction implements CopyAction {
        private final FileResolver resolver;

        public LinkAction(FileResolver res) {
            resolver = res;
        }

        @Override
        public WorkResult execute(CopyActionProcessingStream stream) {
            StreamAction action = new StreamAction();
            stream.process(action);
            return new SimpleWorkResult(action.didWork);
        }

        private class StreamAction implements CopyActionProcessingStreamAction {
            boolean didWork = false;

            @Override
            public void processFile(FileCopyDetailsInternal details) {
                File file = details.getFile();
                Path path = file.toPath();
                File target = resolver.resolve(details.getRelativePath().getPathString());
                Path targetPath = target.toPath();

                Path targetParent = targetPath.getParent();
                try {
                    if (Files.isDirectory(path)) {
                        if (!Files.isDirectory(targetPath)) {
                            logger.info("creating directory {}", targetPath);
                            Files.createDirectory(targetPath);
                        }
                    } else if (Files.exists(targetPath)) {
                        if (!Files.isSameFile(path, targetPath)) {
                            throw new IOException(targetPath + " already exists but is not link");
                        }
                    } else {
                        logger.info("creating hard link {} -> {}", targetParent, path);
                        Files.createLink(targetPath, path);
                        didWork = true;
                    }
                } catch (IOException e) {
                    throw new TaskExecutionException(LinkTree.this, e);
                }
            }
        }
    }
}

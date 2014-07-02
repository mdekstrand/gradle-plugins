package net.elehack.gradle.git

import org.gradle.api.Plugin
import org.gradle.api.Project

class GitPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.extensions.create('git', GitExtension)
    }
}

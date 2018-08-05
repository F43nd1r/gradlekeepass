package com.faendir.gradlekeepass


import org.gradle.api.Plugin
import org.gradle.api.Project
/**
 * @author lukas
 * @since 05.08.18
 */
class KeepassPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('keepass', KeepassExtension, project)
    }
}

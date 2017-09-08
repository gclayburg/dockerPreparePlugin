/*
 * Copyright (c) 2017 Gary Clayburg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.garyclayburg.docker

import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.provider.PropertyState
import org.gradle.api.provider.Provider

/**
 * <br><br>
 * Created 2017-09-03 17:41
 *
 * @author Gary Clayburg
 */
@Slf4j
class DockerLayerPluginExtension {
    /**
     * where we build the docker
     */
    final PropertyState<String> dockerBuildDirectory
    final PropertyState<String> dockerSrcDirectory
    final PropertyState<String> dockerBuildClassesDirectory
    final PropertyState<String> dockerBuildDependenciesDirectory

    DockerLayerPluginExtension(Project project) {
        dockerBuildDirectory = project.property(String)
        dockerSrcDirectory = project.property(String)
        dockerBuildClassesDirectory = project.property(String)
        dockerBuildDependenciesDirectory = project.property(String)
    }

    String getDockerBuildDirectory() {
        dockerBuildDirectory.get()
    }

    String getDockerSrcDirectory() {
        dockerSrcDirectory.get()
    }

    String getDockerBuildClassesDirectory() {
        dockerBuildClassesDirectory.get()
    }

    String getDockerBuildDependenciesDirectory() {
        dockerBuildDependenciesDirectory.get()
    }

    Provider<String> getDockerBuildDirectoryProvider() {
        dockerBuildDirectory
    }

    Provider<String> getDockerSrcDirectoryProvider() {
        dockerSrcDirectory
    }

    Provider<String> getDockerBuildClassesDirectoryProvider() {
        dockerBuildClassesDirectory
    }

    Provider<String> getDockerBuildDependenciesDirectoryProvider() {
        dockerBuildDependenciesDirectory
    }

    /**
     * This directory is the staging area for creating a docker image.  DockerLayerPlugin will populate the files and directories for the docker layers here.
     * <p>Defaults to  "${project.buildDir}/docker"
     * @param dockerBuildDirectory
     */
    void setDockerBuildDirectory(String dockerBuildDirectory) {
        log.info('set docker build dir to: ' + dockerBuildDirectory)
        this.dockerBuildDirectory.set(dockerBuildDirectory)
        this.dockerBuildClassesDirectory.set(dockerBuildDirectory + "/classesLayer")
        this.dockerBuildDependenciesDirectory.set(dockerBuildDirectory + "/dependenciesLayer")
    }

    void setDockerSrcDirectory(String dockerSrcDir) {
        log.info("set docker src dir: " + dockerSrcDir)
        this.dockerSrcDirectory.set(dockerSrcDir)
    }
}

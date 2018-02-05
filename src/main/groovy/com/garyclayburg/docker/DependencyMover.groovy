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

import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * <br><br>
 * Created 2017-10-16 17:11
 *
 * @author Gary Clayburg
 */
class DependencyMover {
    Set foundDependencies = []
    def alreadyMovedJarfileNames = []
    def settings
    Project project

    void createdirs(){
        project.file(settings.commonServiceDependenciesDirectory).mkdirs()
        project.file(settings.dockerBuildDependenciesDirectory).mkdirs()
    }
    void move(String configurationName, String partialPath){
        createdirs()
        matchJars(configurationName).each { commonJarFile ->
            /*
            some dependencies might only exist in providedRuntime configuration
             */
            alreadyMovedJarfileNames.add(commonJarFile.name)
            project.getLogger().info("common $configurationName dependency: "+commonJarFile.name)
            project.ant.move(file: settings.dockerBuildDependenciesDirectory + partialPath + commonJarFile.name,
                    tofile: settings.commonServiceDependenciesDirectory + partialPath + commonJarFile.name)
        }
    }

    void moveProjectJars(String configurationName, String partialPath){
        findDependentProjectJars(configurationName).each { projectJarFile ->
            String dependentProjectJar = settings.dockerBuildDependenciesDirectory + partialPath + projectJarFile.name
            project.getLogger().info('moving dependent project jar to classes layer: '+dependentProjectJar)
            project.ant.move(file: dependentProjectJar,
                    tofile: settings.dockerBuildClassesDirectory + partialPath +projectJarFile.name)
        }

    }

    private Set<File> findDependentProjectJars(String configurationName) {
        Set<File> projectfiles = []
        project.configurations.getByName(configurationName).allDependencies.findAll { dep ->
            dep instanceof ProjectDependency
        }.each { projectdep ->
            ResolvedArtifact depArtifact = project.configurations.getByName(configurationName).resolvedConfiguration.resolvedArtifacts.find {
                return it.name == projectdep.name && it.moduleVersion.id.group == projectdep.group
            }
            project.getLogger().info(" dependent project jar file found: " + depArtifact?.file)
            projectfiles.add(depArtifact?.file)
        }
        projectfiles
    }

    void moveWar(String configurationName){
        createdirs()
        matchJars(configurationName).each { commonJarFile ->
            if (!alreadyMovedJarfileNames.contains(commonJarFile.name)){
                project.getLogger().info("common $configurationName dependency: "+commonJarFile.name)
                /*
            a configuration like:
            dockerprepare {
              commonService = ['org.springframework.boot:spring-boot-starter-web']
            }
            might have some transitive jar files in /WEB-INF/lib-provided and some in /WEB-INF/lib
             */
                File srcFileProvidedRuntime = new File((String) settings.dockerBuildDependenciesDirectory + '/WEB-INF/lib-provided/' + commonJarFile.name)
                if (srcFileProvidedRuntime.exists()){
                    project.ant.move(file: srcFileProvidedRuntime,
                            tofile: settings.commonServiceDependenciesDirectory + "/WEB-INF/lib-provided/" + commonJarFile.name)

                } else{
                    project.ant.move(file: settings.dockerBuildDependenciesDirectory + "/WEB-INF/lib/" + commonJarFile.name,
                            tofile: settings.commonServiceDependenciesDirectory + "/WEB-INF/lib/" + commonJarFile.name)
                }
            }
        }

    }

    private Set<File> matchJars(String configurationName) {
        project.configurations.getByName(configurationName).files { providedRuntimeDependency ->
            def groupname = providedRuntimeDependency.group + ":" + providedRuntimeDependency.name
            project.getLogger().info("matchJars checking: $providedRuntimeDependency")
            def matchedDep = settings.commonService?.contains(groupname)
            if (matchedDep) {
                foundDependencies.add(groupname)
            }
            matchedDep
        }
    }

    void check(){
        if (foundDependencies.size() != settings.commonService?.size()){
            throw new IllegalStateException("""
WARNING!!! one or more commonService dependencies not found in your project 
commonService = $settings.commonService 
but,
found =         $foundDependencies"""
            )
        } else{
            project.getLogger().info("all commonService dependencies found")
        }

    }
}

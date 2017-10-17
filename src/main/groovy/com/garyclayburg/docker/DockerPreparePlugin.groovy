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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar

/**
 * <br><br>
 * Created 2017-09-03 17:36
 *
 * @author Gary Clayburg
 */
@Slf4j
class DockerPreparePlugin implements Plugin<Project> {
    public static final String COPY_DEFAULT_DOCKERFILE = 'copyDefaultDockerfile'
    public static final String COPY_DOCKER = 'copyDocker'
    public static final String EXPAND_BOOT_JAR = 'expandBootJar'
    public static final String DOCKER_LAYER_PREPARE = 'dockerLayerPrepare'
    public static final String DOCKERPREPARE_EXTENSION = "dockerprepare"
    Project project
    def settings
    boolean usingSpringBoot2API = false
    private Task bootRepackageTask

    @Override
    void apply(Project project) {
        this.project = project
        project.getLogger().info ' apply com.garyclayburg.dockerprepare'
        settings = createExtension()
        project.afterEvaluate { afterevalproject ->
            probeSpringBootVersion(afterevalproject)
            failWhenExecutable()
            def jarTask = afterevalproject.tasks.getByName('jar')
            def warT = getWarTask(afterevalproject)
            def expandBootJarTask = afterevalproject.task(EXPAND_BOOT_JAR) {
                inputs.files {
                    warT != null ? [jarTask.archivePath,warT.archivePath] :[jarTask.archivePath]
                }
                outputs.dir { settings.dockerBuildDependenciesDirectory }
                doLast {
                    File jarfile = project.file(jarTask.archivePath)
                    if (jarfile.exists()){
                        getLogger().info("jar file populating dependencies layer ${settings.dockerBuildDependenciesDirectory} from \n${jarTask.archivePath}")
                        //in some projects, jar.archivePath may change after bootRepackage is executed.
                        // It might be one value during configure, but another after bootRepackage.
                        afterevalproject.copy {
                            from afterevalproject.zipTree(jarTask.archivePath)
                            into settings.dockerBuildDependenciesDirectory
                            exclude "/BOOT-INF/classes/**"
                            exclude "/META-INF/**"
                        }
                        getLogger().info("jar file populating classes layer ${settings.dockerBuildClassesDirectory} from \n${jarTask.archivePath}")
                        afterevalproject.copy {
                            from afterevalproject.zipTree(jarTask.archivePath)
                            into settings.dockerBuildClassesDirectory
                            include "/BOOT-INF/classes/**"
                            include "/META-INF/**"
                        }
                        moveCommonJar()
                    } else {
                        def warTask
                        try {
                            warTask = afterevalproject.tasks.getByName('war')
                            File warfile = project.file(warTask.archivePath)

                            if (warfile.exists()){
                                getLogger().info("war file populating dependencies layer ${settings.dockerBuildDependenciesDirectory} from \n${warTask.archivePath}")
                                //in some projects, jar.archivePath may change after bootRepackage is executed.
                                // It might be one value during configure, but another after bootRepackage.
                                afterevalproject.copy {
                                    from afterevalproject.zipTree(warTask.archivePath)
                                    into settings.dockerBuildDependenciesDirectory
                                    include "/WEB-INF/lib*/**"
                                }
                                getLogger().info("war file populating classes layer ${settings.dockerBuildClassesDirectory} from \n${warTask.archivePath}")
                                afterevalproject.copy {
                                    from afterevalproject.zipTree(warTask.archivePath)
                                    into settings.dockerBuildClassesDirectory
                                    exclude "WEB-INF/lib*/**"
                                }
                                moveCommonWar()
                            } else{
                                getLogger().error("no war file or jar file found to prepare for Docker")
                            }
                        } catch (UnknownTaskException ignore) {
                            getLogger().error("no war file or jar file found to prepare for Docker")
                        }
                    }
                }
            }
            expandBootJarTask.setDependsOn([bootRepackageTask])
            createDockerPrepare(DOCKER_LAYER_PREPARE)
            afterevalproject.getTasks().getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn(DOCKER_LAYER_PREPARE)
        }
        createCopydocker(COPY_DOCKER)
        createCopyDefaultDockerfile(COPY_DEFAULT_DOCKERFILE)

    }

    private void moveCommonJar() {
        DependencyMover dm = new DependencyMover(settings: settings,project: project)
        dm.move('runtime','/BOOT-INF/lib/')
        dm.check()
    }

    private void moveCommonWar() {
        DependencyMover dm = new DependencyMover(settings: settings,project: project)
        dm.move('providedRuntime','/WEB-INF/lib-provided/')
        dm.moveWar('runtime')
        dm.check()
    }

    private void failWhenExecutable() {
        def springBootExtension
        project.getLogger().info('checking springboot execution')
        springBootExtension = project.extensions.findByName('springBoot')
        if (springBootExtension != null){
            if (!usingSpringBoot2API){
                /*
                This is an attempt for this project to be as flexible as possible.
                 We only need a few things from the spring boot gradle plugin, so we
                 try to allow the user to choose the version they want in their
                 own build.gradle.
                This part may be a little fragile, but it is probably better than
                 this project depending on a specific version of Spring Boot.
                 */
                if (springBootExtension.isExecutable()){
                    throw new IllegalStateException("dockerprepare cannot prepare jar/war file that is executable.  See https://github.com/gclayburg/dockerPreparePlugin#user-content-errors")
                }
            } else{
                assert project.bootJar instanceof Jar
                if (project.bootJar.getLaunchScript().isIncluded()){
                    throw new IllegalStateException("dockerprepare cannot prepare spring boot 2 jar/war file that is executable.  See https://github.com/gclayburg/dockerPreparePlugin#user-content-errors")
                }
            }
        } else{
            throw new IllegalStateException("springBoot extension not found")
        }

    }

    private DockerPreparePluginExt createExtension() {
        settings = project.extensions.create(DOCKERPREPARE_EXTENSION, DockerPreparePluginExt, project)
        settings.dockerSrcDirectory = "${project.rootDir}/src/main/docker"
        settings.dockerBuildDirectory = "${project.buildDir}/docker"
        settings
    }

    private void createDockerPrepare(String name) {
        def dockerPrep = project.task(name)
        dockerPrep.dependsOn(EXPAND_BOOT_JAR, COPY_DOCKER, COPY_DEFAULT_DOCKERFILE)
        dockerPrep.setDescription("prepare docker layer-friendly directory from spring boot jar")
        dockerPrep.setGroup("Docker")
    }

    private createCopyDefaultDockerfile(String name) {
        project.task(name) {
            outputs.files {
                [settings.dockerBuildDirectory + "/Dockerfile", settings.dockerBuildDirectory + "/bootrunner.sh"]
            }
            doLast {
                def dockerstream = this.getClass().getResourceAsStream('/defaultdocker/Dockerfile')
                def bootrunnerstream = this.getClass().getResourceAsStream('/defaultdocker/bootrunner.sh')
                if (dockerstream != null && bootrunnerstream != null) {
                    getLogger().info "Copy opinionated default Dockerfile and bootrunner.sh into ${settings.dockerBuildDirectory} "
                    project.mkdir(settings.dockerBuildDirectory)
                    project.file(settings.dockerBuildDirectory + '/Dockerfile') << dockerstream.text
                    project.file(settings.dockerBuildDirectory + '/bootrunner.sh') << bootrunnerstream.text
                    /* this copy from stream technique is consistent whether we are either:
                      1. running as normal where Dockerfile is found inside a jar file on the classpath
                      2. testing via gradle test kit where Dockerfile is a normal file on the classpath
                     */
                } else {
                    getLogger().error('Cannot copy opinionated default Dockerfile and bootrunner.sh')
                    project.buildscript.configurations.classpath.findAll {
                        getLogger().error "classpath entry ${it.path}"
                    }
                    printDir(project.buildDir.getPath(), getLogger())
                }
            }
        }.onlyIf {
            def file = project.file(settings.dockerSrcDirectory)
            (!file.isDirectory() || (file.list().length == 0))
        }
    }

    private createCopydocker(String name) {
        project.task(name, type: Copy) {
            from { project.dockerprepare.dockerSrcDirectory }
            //lazy evaluation via closure so that dockerprepare settings can be overridden in build.properties
            into { project.dockerprepare.dockerBuildDirectory }
            doLast {
                def mysrc = { project.dockerprepare.dockerSrcDirectory }
                def myto = { project.dockerprepare.dockerBuildDirectory }
                getLogger().info("Copying docker file(s) from ${mysrc()} to:\n${myto()}")
            }
        }.onlyIf {
            def file = project.file(settings.dockerSrcDirectory)
            (file.isDirectory() && (file.list().length != 0))
        }
    }

    private Task getWarTask(Project afterevalproject) {
        def warT = null
        try {
            warT = afterevalproject.tasks.getByName('war')
        } catch (UnknownTaskException ignored) {
        }
        warT
    }

    private void probeSpringBootVersion(Project afterevalproject) {
        //add expandBootJar task and bootRepackage dependencies after other plugins are evaluated
        //we depend on the 'jar' task from java plugin and 'bootRepackage' from spring boot gradle plugin
        try {
            this.bootRepackageTask = afterevalproject.tasks.getByName('bootRepackage')
            this.usingSpringBoot2API = false
            project.getLogger().info('using spring boot 1...')
        } catch (UnknownTaskException ignore) {
            try {
                this.bootRepackageTask = afterevalproject.tasks.getByName('bootJar')
                usingSpringBoot2API = true
                project.getLogger().info('using spring boot 2...')
            } catch (UnknownTaskException ute2) {
                project.getLogger().error('com.garyclayburg.dockerprepare gradle plugin requires Spring Boot plugin, however \'bootRepackage\' task does not exist.  Is Spring Boot installed correctly?')
                throw ute2
            }
        }
    }

    private static void printDir(String path, Logger logger) {
        File rootDir = new File(path)
        if (rootDir.exists() && rootDir.isDirectory()) {
            rootDir.traverse {
                logger.error "f ${it}"
            }
        }
    }
}

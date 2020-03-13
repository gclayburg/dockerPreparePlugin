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
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar

/**
 * <br>
 * Prepares a <a href="https://projects.spring.io/spring-boot/">Spring Boot</a> application for deployment in a docker container<br>
 * <h3>Features:</h3>
 * <ul>
 *     <li> splits Spring Boot jar or war into docker cache-friendly layers
 *     <li> includes default Dockerfile that is fast, efficient and secure
 *     <li> application running in docker will honor OS signals like SIGQUIT
 *     <li> application running in docker can process command line parameters:
 *     <pre>
 *   docker run registry:5000/multiprojectdemo:1.0 --info.app.name="widget instance 1"
 *       </pre>
 * </ul>
 *
 * The defaults can be overridden with an optional {@link DockerPreparePluginExt dockerprepare} block:
 * <pre>
 *    dockerprepare {
 *      commonService = ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-starter-actuator']
 *    }
 *     </pre>
 *
 *
 *  More details on <a href="https://github.com/gclayburg/dockerPreparePlugin">github</a>
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
    DockerPreparePluginExt settings
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
// todo fix deprecated jarTask.archivePath references
// deprecated newer gradle, the replacement jarTask.archiveFile
// does not exist in oldish gradle versions
//                println "   jarTask.archivePath $jarTask.archivePath"
//                println "   bootRepackageTask.archiveFile $bootRepackageTask.archiveFile"
//                println "   bootRepackageTask.archivePath $bootRepackageTask.archivePath"
                File jarArchivePath = insertClassifier(jarTask.archivePath,bootRepackageTask.classifier)
                inputs.files {
                    warT != null ? [jarArchivePath, insertClassifier(warT.archivePath,bootRepackageTask.classifier)] : [jarArchivePath]
                }
                outputs.dir { settings.dockerBuildDependenciesDirectory }
                doLast {
                    File jarfile = project.file(jarArchivePath)
                    if (jarfile.exists()) {
                        getLogger().info("jar file populating dependencies layer ${settings.dockerBuildDependenciesDirectory} from \n${jarArchivePath}")
                        //in some projects, jar.archivePath may change after bootRepackage is executed.
                        // It might be one value during configure, but another after bootRepackage.
                        afterevalproject.copy {
                            from afterevalproject.zipTree(jarArchivePath)
                            into settings.dockerBuildDependenciesDirectory
                            exclude "/BOOT-INF/classes/**"
                            exclude "/META-INF/**"
                        }
                        getLogger().info("jar file populating classes layer ${settings.dockerBuildClassesDirectory} from \n${jarArchivePath}")
                        afterevalproject.copy {
                            from afterevalproject.zipTree(jarArchivePath)
                            into settings.dockerBuildClassesDirectory
                            include "/BOOT-INF/classes/**"
                            include "/META-INF/**"
                        }
                        moveCommonJar()
                        moveSnapShots()
                    } else {
                        def warTask
                        try {
                            warTask = afterevalproject.tasks.getByName('war')
                            File warTaskArchivePath = insertClassifier(warTask.archivePath,bootRepackageTask.classifier)
                            File warfile = project.file(warTaskArchivePath)

                            if (warfile.exists()) {
                                getLogger().info("war file populating dependencies layer ${settings.dockerBuildDependenciesDirectory} from \n${warTaskArchivePath}")
                                //in some projects, jar.archivePath may change after bootRepackage is executed.
                                // It might be one value during configure, but another after bootRepackage.
                                afterevalproject.copy {
                                    from afterevalproject.zipTree(warTaskArchivePath)
                                    into settings.dockerBuildDependenciesDirectory
                                    include "/WEB-INF/lib*/**"
                                }
                                getLogger().info("war file populating classes layer ${settings.dockerBuildClassesDirectory} from \n${warTaskArchivePath}")
                                afterevalproject.copy {
                                    from afterevalproject.zipTree(warTaskArchivePath)
                                    into settings.dockerBuildClassesDirectory
                                    exclude "WEB-INF/lib*/**"
                                }
                                moveCommonWar()
                                moveSnapShots()
                            } else {
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
        DependencyMover dm = new DependencyMover(settings: settings, project: project)
        dm.move('runtime', '/BOOT-INF/lib/')
        dm.moveProjectJars('runtime', '/BOOT-INF/lib/')
        dm.check()
    }

    private void moveCommonWar() {
        DependencyMover dm = new DependencyMover(settings: settings, project: project)
        dm.move('providedRuntime', '/WEB-INF/lib-provided/')
        dm.moveWar('runtime')
        dm.moveProjectJars('runtime', '/WEB-INF/lib/')
        dm.check()
    }

    private void failWhenExecutable() {
        def springBootExtension
        project.getLogger().info('checking springboot execution')
        springBootExtension = project.extensions.findByName('springBoot')
        if (springBootExtension != null) {
            if (!usingSpringBoot2API) {
                /*
                This is an attempt for this project to be as flexible as possible.
                 We only need a few things from the spring boot gradle plugin, so we
                 try to allow the user to choose the version they want in their
                 own build.gradle.
                This part may be a little fragile, but it is probably better than
                 this project depending on a specific version of Spring Boot.
                 */
                if (springBootExtension.isExecutable()) {
                    throw new IllegalStateException("dockerprepare cannot prepare jar/war file that is executable.  See https://github.com/gclayburg/dockerPreparePlugin#user-content-errors")
                }
            } else {
                assert project.bootJar instanceof Jar
                def script = project.bootJar.getLaunchScript()
                if (script != null && script.isIncluded()) {
                    throw new IllegalStateException("dockerprepare cannot prepare spring boot 2 jar/war file that is executable.  See https://github.com/gclayburg/dockerPreparePlugin#user-content-errors")
                }
            }
        } else {
            throw new IllegalStateException("springBoot extension not found")
        }

    }

    private DockerPreparePluginExt createExtension() {
        settings = project.extensions.create(DOCKERPREPARE_EXTENSION, DockerPreparePluginExt, project)
        settings.snapshotLayer = false
        settings.dockerSrcDirectory = "${project.projectDir}/src/main/docker"
        settings.dockerBuildDirectory = "${project.buildDir}/docker"
        settings.dockerfileSet = "defaultdocker"
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
                def dockerfile
                def bootrunnerfile
                if (settings.snapshotLayer){
                    dockerfile = "/4layers/${settings.dockerfileSet}/Dockerfile"
                    bootrunnerfile = "/4layers/${settings.dockerfileSet}/bootrunner.sh"
                } else {
                    dockerfile = "/${settings.dockerfileSet}/Dockerfile"
                    bootrunnerfile = "/${settings.dockerfileSet}/bootrunner.sh"
                }
                def dockerstream = this.getClass().getResourceAsStream(dockerfile)
                def bootrunnerstream = this.getClass().getResourceAsStream(bootrunnerfile)
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
                    getLogger().error("Cannot copy opinionated default Dockerfile and bootrunner.sh from classpath \n  ${dockerfile}\n  ${bootrunnerfile}")
                    project.buildscript.configurations.classpath.findAll {
                        getLogger().error "classpath entry ${it.path}"
                    }
                    printDir(project.buildDir.getPath(), getLogger())
                    throw new IllegalStateException('Cannot copy opinionated default Dockerfile and bootrunner.sh')
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

    /*
    e.g. with no classifier:
    ./scanrunner/build/libs/scanrunner-0.7.8-SNAPSHOT.jar
    with classifier 'boot':
    ./scanrunner/build/libs/scanrunner-0.7.8-SNAPSHOT-boot.jar

     */
    static File insertClassifier(File archivePath, String classifier) {
        def outputFile = archivePath
        if (classifier != null && classifier != '') {
            def filePattern = ~/(.*)(\.[jw]ar)$/
            def matcher = archivePath.path =~ filePattern
            if (matcher.find()) {
                def newFileName = matcher.group(1) + '-' + classifier + matcher.group(2)
                outputFile = new File(newFileName)
            }
        }
        outputFile
    }

    static String getSnapshotPath(String path) {
        def snapshotpattern = ~/(.*)dependenciesLayer2(.*)$/
        def matcher = path =~ snapshotpattern
        def newFileName = null
        if (matcher.find()) {
            newFileName = matcher.group(1) + DockerPreparePluginExt.SNAPSHOT_LAYER3 + matcher.group(2)
        }
        return newFileName
    }

    void moveSnapShots() {
        ConfigurableFileTree tree = project.fileTree(settings.dockerBuildDependenciesDirectory) {
            include '**/*-SNAPSHOT.jar'
        }
        tree.files.each { snapshotfile ->
            if (settings.getSnapshotLayer()) {
                project.ant.move(file: snapshotfile.path, toFile: project.file(getSnapshotPath(snapshotfile.path)))
            } else {
                project.getLogger().warn('WARNING: {} is a SNAPSHOT dependency.  Consider enabling snapshot layer for better storage efficiency',snapshotfile.name)
            }

        }
    }
}

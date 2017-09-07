package com.garyclayburg.docker

import groovy.util.logging.Slf4j
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownTaskException
import org.gradle.api.tasks.Copy

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * <br><br>
 * Created 2017-09-03 17:36
 *
 * @author Gary Clayburg
 */
@Slf4j
class DockerLayerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getLogger().info ' apply com.garyclayburg.dockerprepare'
        def extension = project.extensions.create("dockerlayer", DockerLayerPluginExtension, project)
        extension.dockerSrcDirectory = "${project.rootDir}/src/main/docker"
        extension.dockerBuildDirectory = "${project.buildDir}/docker"
        boolean prereqsmet = true
        def bootRepackageTask
        try {
            bootRepackageTask = project.tasks.getByName('bootRepackage')
        } catch (UnknownTaskException ute) {
            project.getLogger().error('\'bootRepackage\' task does not exist so there is nothing to do.  Is spring boot installed correctly?', ute)
            prereqsmet = false
        }
        if (prereqsmet) {
            def jarTask = project.tasks.getByName('jar')
            project.task('copyDocker', type: Copy) {
                from extension.dockerSrcDirectoryProvider
                into extension.dockerBuildDirectoryProvider
                doLast {
                    getLogger().info("Copy docker file(s) from ${extension.dockerSrcDirectoryProvider.get()} to:\n${extension.dockerBuildDirectoryProvider.get()}")
                }
            }.onlyIf {
                def file = project.file(extension.dockerSrcDirectoryProvider.get())
                (file.isDirectory() && (file.list().length != 0))
            }
            project.task('copyDefaultDockerfile') {
                doLast {
                    def myjar = project.buildscript.configurations.classpath.find {
                        it.name.contains 'dockerPreparePlugin'
                    }
                    if (myjar != null) {
                        getLogger().info "Copy opinionated default Dockerfile and bootrunner.sh into ${extension.dockerBuildDirectoryProvider.get()}"
                        project.copy {
                            from project.resources.text.fromArchiveEntry(myjar,
                                    '/defaultdocker/Dockerfile').asFile()
                            into extension.dockerBuildDirectoryProvider
                        }
                        project.copy {
                            from project.resources.text.fromArchiveEntry(myjar,
                                    '/defaultdocker/bootrunner.sh').asFile()
                            into extension.dockerBuildDirectoryProvider
                        }
                    } else {
                        getLogger().error('Cannot copy opinionated default Dockerfile and bootrunner.sh')
                        getLogger().info "classpath files " + project.buildscript.configurations.classpath.findAll {
                            true
                        }
                    }
                }
            }.onlyIf {
                def file = project.file(extension.dockerSrcDirectoryProvider.get())
                (!file.isDirectory() || (file.list().length == 0))
            }
            project.task('copyClasses') {
                doLast {
                    getLogger().info("populating classes layer ${extension.dockerBuildClassesDirectoryProvider.get()} from \n${jarTask.archivePath}")
                    //in some projects, jar.archivePath may change after bootRepackage is executed.
                    // It might be one value during configure, but another after bootRepackage.
                    // We use the project.copy method instead of the Copy task so our task can
                    // get the correct value during the execute phase
                    project.copy {
                        from project.zipTree(jarTask.archivePath)
                        into extension.dockerBuildClassesDirectoryProvider
                        include "/BOOT-INF/classes/**"
                        include "/META-INF/**"
                    }
                }
            }.setDependsOn([bootRepackageTask])
            project.task('copyDependencies') {
                doLast {
                    getLogger().info("populating dependencies layer ${extension.dockerBuildDependenciesDirectoryProvider.get()} from \n${jarTask.archivePath}")
                    //in some projects, jar.archivePath may change after bootRepackage is executed.
                    // It might be one value during configure, but another after bootRepackage.
                    // We use the project.copy method instead of the Copy task so our task can
                    // get the correct value during the execute phase
                    project.copy {
                        from project.zipTree(jarTask.archivePath)
                        into extension.dockerBuildDependenciesDirectoryProvider
                        exclude "/BOOT-INF/classes/**"
                        exclude "/META-INF/**"
                    }
                }
            }.setDependsOn([bootRepackageTask])
            def dockerPrep = project.task('dockerPrepare')
            dockerPrep.dependsOn('copyClasses', 'copyDependencies', 'copyDocker', 'copyDefaultDockerfile')
        }
    }
}

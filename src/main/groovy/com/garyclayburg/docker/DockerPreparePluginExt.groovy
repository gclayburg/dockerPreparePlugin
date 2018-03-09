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

/**
 * <h2>properties</h2>
 * <ul>
 *     <li> {@link #commonService} </li>
 *     <li> {@link #dockerSrcDirectory} </li>
 *     <li> {@link #dockerBuildDirectory} </li>
 *     <li> {@link #dockerBuildDependenciesDirectory} </li>
 *     <li> {@link #commonServiceDependenciesDirectory} </li>
 * </ul>
 * <h2>Customization examples</h2>
 * <b>Put the tomcat jar files and actuator jar files into into the common layer:</b>
 * <blockquote><pre>
 * dockerprepare {
 *   commonService = ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-starter-actuator']
 * }
 * </pre></blockquote>
 * <b>Place your own Dockerfile in this directory to override the <a href="#defaultdocker">default:</a></b>
 * <pre>
 * ${project.rootDir}/src/main/docker/
 * </pre>
 *
 * <hr>
 * <b>Tell the plugin to use your own provided </b> {@code src/main/dockerroot/Dockerfile}:
 * <blockquote><pre>
 * dockerprepare {
 *   commonService = ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-starter-actuator']
 *   dockerSrcDirectory = "${project.rootDir}/src/main/dockerroot/
 * }
 * </pre></blockquote>
 * <hr>
 * <b>Complete build.gradle with integration with <a href="https://github.com/bmuschko/gradle-docker-plugin">docker-remote-api</a> to create docker image from our prepared {@link #dockerBuildDirectory}</b>
 * <blockquote><pre>
 * plugins {
 *     id 'org.springframework.boot' version '1.5.10-RELEASE'
 *     id 'com.garyclayburg.dockerprepare' version '1.2.1'
 *     id "com.bmuschko.docker-remote-api" version "3.1.0"
 *     id 'java'
 *     id 'eclipse'
 * }
 *
 * version = '0.0.1-SNAPSHOT'
 * sourceCompatibility = 1.8
 *
 * repositories {
 *     mavenCentral()
 * }
 *
 * dockerprepare {
 *     commonService = ['org.springframework.boot:spring-boot-starter-web']
 * }
 *
 * dependencies {
 *     compile('org.springframework.boot:spring-boot-starter-web')
 *     testCompile('org.springframework.boot:spring-boot-starter-test')
 * }
 * def dockerImageName = 'myorg/' + project.name
 *
 * task buildImage(type: com.bmuschko.gradle.docker.tasks.image.DockerBuildImage, dependsOn:
 *        'dockerLayerPrepare') {
 *     description = "build and tag a Docker Image"
 *     inputDir = project.file(dockerprepare.dockerBuildDirectory)
 *     tags = [dockerImageName + ':latest', dockerImageName + ':' + version]
 * }
 *
 * task pushVersion(type: com.bmuschko.gradle.docker.tasks.image.DockerPushImage, dependsOn:
 *        buildImage) {
 *     description = "docker push &ltimageName&gt:&ltversion&gt"
 *     imageName = dockerImageName + ":" + version
 * }
 *
 * task pushLatest(type: com.bmuschko.gradle.docker.tasks.image.DockerPushImage, dependsOn:
 *       buildImage) {
 *     description = "docker push &ltimageName&gt:latest"
 *     imageName = dockerImageName + ":latest"
 * }
 *
 * </pre></blockquote
 * <hr>
 *     <h2><a name="defaultdocker">Default Dockerfile</a></h2>
 *     <pre>
 FROM openjdk:8u131-jre-alpine
 # We choose this base image because:
 # 1. it is the latest Java 8 version on alpine as of September 2017
 # 2. jre-alpine instead of jdk-alpine is much smaller but still enough to
 #    run most microservice applications on the JVM
 # 3. jre-alpine has a smaller security footprint than other official Java docker images
 # 4. the explicit version number means the build will be repeatable
 #    i.e. not dependent on what :latest version may have been pulled from a
 #    docker registry before.

 RUN adduser -D -s /bin/sh springboot
 COPY ./bootrunner.sh /home/springboot/bootrunner.sh
 RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
 WORKDIR /home/springboot
 USER springboot
 # We add a special springboot user for running our application.
 # Java applications do not need to be run as root

 ADD commonServiceDependenciesLayer1 /home/springboot/app/
 # This layer is composed of all transitive dependencies of a
 # commonService, e.g in your build.gradle:
 #
 #dockerprepare {
 #  commonService = ['org.springframework.boot:spring-boot-starter-web']
 #}
 #
 # All 30 jar files pulled in from spring-boot-starter-web are added to this layer

 ADD dependenciesLayer2/ /home/springboot/app/
 # This layer contains dependent jar files of the app that aren't a
 # commonService.  Most of the time,
 # having dependencies in this layer will take advantage of the docker build
 # cache.  This will give you faster build times, faster image
 # uploads/downloads and reduced storage requirements.
 # This layer is computed automatically from your spring boot application

 ADD classesLayer3/ /home/springboot/app/
 # This layer contains your application classes.  It will
 # likely change on each docker image build so we expect a docker cache miss.
 # This layer is computed automatically from your spring boot application

 VOLUME /tmp
 EXPOSE 8080
 ENV JAVA_OPTS=""
 ENTRYPOINT ["./bootrunner.sh"]
</pre>
 <hr>
 *     <h2>Default bootrunner.sh</h2>
 *     <pre>
#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
#exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully
if [ -d "${HOME}/app" ]; then
  #execute springboot expanded jar, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.JarLauncher $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.JarLauncher "$@"
elif [ -f "${HOME}/app.jar" ]; then
  # execute springboot jar
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar ${HOME}/app.jar $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${HOME}/app.jar" "$@"
else
  date_echo "springboot application not found in ${HOME}/app or ${HOME}/app.jar"
  exit 1
fi
 * </pre>
 *
 * @author Gary Clayburg
 */
@Slf4j
class DockerPreparePluginExt {
    final Project project
/*
Note: groovydoc here is duplicated 3 times for each property to allow Intellij quick
documentation to work in a variety of cases
 */
    /**
     * All files or directories in this directory will be copied to the {@link #dockerBuildDirectory}.  If you want to use a custom Dockerfile to
     * create your image, it should be placed here.
     * <p>If the directory does not exist or is empty, a default Dockerfile
     * and start script for your app will be placed in the
     * {@link #dockerBuildDirectory}
     * <p> Defaults to "${project.rootDir}/src/main/docker/"
     * @param sourceDirectory Source directory for adding files to docker
     */
    String dockerSrcDirectory

    /**
     * This directory is the output of this plugin that represents your Spring Boot
     * application in a docker layer-friendly format.  This directory should be used
     * as a context for creating a docker image.  For example, to create the docker
     * image from this directory manually:
     *
     * <pre>
     * $ docker build -t myname/myapp .
     * </pre>
     * <p>Defaults to: ${project.buildDir}/docker
     */
    String dockerBuildDirectory

    /**
     * Build directory where classes of this project are placed.  The directory name must
     * match the Directory in your Dockerfile.
     * <p>${project.buildDir}/docker/classesLayer3
     */
    String dockerBuildClassesDirectory
    /**
     * Build directory where dependencies of this project are placed.  The directory name must
     * match the Directory in your Dockerfile.
     * <p>${project.buildDir}/docker/dependenciesLayer2
     */
    String dockerBuildDependenciesDirectory
    /**
     * Build directory where common service dependencies are placed.  The directory name must
     * match the Directory in your Dockerfile.
     * <p>${project.buildDir}/docker/commonServiceDependenciesLayer1
     */
    String commonServiceDependenciesDirectory

    /**
     * Marks dependencies to be placed in the top layer of a docker image, e.g.
     * <pre>
     *     commonService =  ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-actuator']
     * </pre>
     *
     * This example will place all the jar files associated with the embedded tomcat server
     * in the top layer of the docker image. This should allow for better docker cache hits
     * when you have several independent projects/docker images that share this same
     * commonService setting
     * @param commonService list of dependencies common to many projects
     */
    List<String> commonService = []

    /**
     * Instead of using the default {@code Dockerfile} and {@code bootrunner.sh}, you can specify a different
     * pre-packaged {@code Dockerfile} and {@code bootrunner.sh}:
     *
     * <br><br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/90111-jre-sid-buildlabels">90111-jre-sid-buildlabels</a>
     * <pre>
     *     dockerfileSet = '90111-jre-sid-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/8u151-jre-alpine-buildlabels">8u151-jre-alpine-buildlabels</a>
     * <pre>
     *     dockerfileSet = '8u151-jre-alpine-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/defaultdocker">defaultdocker</a>
     * <pre>
     *     dockerfileSet = 'defaultdocker'
     * </pre>
     *
     * If not specified, the {@code Dockerfile} and {@code bootrunner.sh} file from the dockerfileSet defaultdocker will be used, unless you also have files in {@link #dockerSrcDirectory}

     */
    String dockerfileSet

    DockerPreparePluginExt(Project project) {
        this.project = project
    }

    DockerPreparePluginExt() {
    }

    def dockerprepare(Closure closure) { //method name matches config block override:
        closure.delegate = this
        closure()
    }

    /**
     * Marks dependencies to be placed in the top layer of a docker image, e.g.
     * <pre>
     *     commonService =  ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-actuator']
     * </pre>
     *
     * This example will place all the jar files associated with the embedded tomcat server
     * in the top layer of the docker image. This should allow for better docker cache hits
     * when you have several independent projects/docker images that share this same
     * commonService setting
     * @param commonService list of dependencies common to many projects
     */
    void setCommonService(String[] commonService) {
        this.commonService(commonService)
    }

    /**
     * Marks dependencies to be placed in the top layer of a docker image, e.g.
     * <pre>
     *     commonService =  ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-actuator']
     * </pre>
     *
     * This example will place all the jar files associated with the embedded tomcat server
     * in the top layer of the docker image. This should allow for better docker cache hits
     * when you have several independent projects/docker images that share this same
     * commonService setting
     * @param commonService list of dependencies common to many projects
     */
    void commonService(String[] commonService) {
        this.commonService = commonService
        commonService.each {
            def (desiredgroup, desiredname) = it.tokenize(':')
            if (desiredgroup == null || desiredname == null) {
                throw new IllegalStateException("Invalid commonService: $commonService. \n   Dependencies must use standard list format, e.g:  ['org.springframework.boot:spring-boot-starter-web','org.springframework.boot:spring-boot-starter-actuator']")
            }

        }
    }

    /**
     * All files or directories in this directory will be copied to the {@link #dockerBuildDirectory}.  If you want to use a custom Dockerfile to
     * create your image, it should be placed here.
     * <p>If the directory does not exist or is empty, a default Dockerfile
     * and start script for your app will be placed in the
     * {@link #dockerBuildDirectory}
     * <p> Defaults to "${project.rootDir}/src/main/docker/"
     * @param sourceDirectory Source directory for adding files to docker
     */
    void setDockerSrcDirectory(String dockerSrcDirectory) {
        this.dockerSrcDirectory(dockerSrcDirectory)
    }

    /**
     * All files or directories in this directory will be copied to the {@link #dockerBuildDirectory}.  If you want to use a custom Dockerfile to
     * create your image, it should be placed here.
     * <p>If the directory does not exist or is empty, a default Dockerfile
     * and start script for your app will be placed in the
     * {@link #dockerBuildDirectory}
     * <p> Defaults to "${project.rootDir}/src/main/docker/"
     * @param sourceDirectory Source directory for adding files to docker
     */
    void dockerSrcDirectory(String sourceDirectory) { //called when closure is applied
        log.info("set dockerSrcDirectory to ${sourceDirectory}")
        this.dockerSrcDirectory = sourceDirectory

    }

    /* used when:
    dockerprepare {
      dockerBuildDirectory = "/"
    }
    */
    /**
     * This directory is the output of this plugin that represents your Spring Boot
     * application in a docker layer-friendly format.  This directory should be used
     * as a context for creating a docker image.  For example, to create the docker
     * image from this directory manually:
     *
     * <pre>
     * $ docker build -t myname/myapp .
     * </pre>
     * <p>Defaults to: ${project.buildDir}/docker
     */
    void setDockerBuildDirectory(String dockerBuildDirectory) {
        this.dockerBuildDirectory(dockerBuildDirectory)
    }

    /* used when:
    dockerprepare {
      dockerBuildDirectory "/"
    }
     */
    /**
     * This directory is the output of this plugin that represents your Spring Boot
     * application in a docker layer-friendly format.  This directory should be used
     * as a context for creating a docker image.  For example, to create the docker
     * image from this directory manually:
     *
     * <pre>
     * $ docker build -t myname/myapp .
     * </pre>
     * <p>Defaults to: ${project.buildDir}/docker
     */
    void dockerBuildDirectory(String dockerBuildDirectory) {
        log.info("set dockerBuildDirectory to ${dockerBuildDirectory}")
        this.dockerBuildDirectory = dockerBuildDirectory
        this.dockerBuildClassesDirectory = dockerBuildDirectory + "/classesLayer3"
        this.dockerBuildDependenciesDirectory = dockerBuildDirectory + "/dependenciesLayer2"
        this.commonServiceDependenciesDirectory = dockerBuildDirectory + "/commonServiceDependenciesLayer1"
    }

    /**
     * Instead of using the default {@code Dockerfile} and {@code bootrunner.sh}, you can specify a different
     * pre-packaged {@code Dockerfile} and {@code bootrunner.sh}:
     *
     * <br><br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/90111-jre-sid-buildlabels">90111-jre-sid-buildlabels</a>
     * <pre>
     *     dockerfileSet = '90111-jre-sid-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/8u151-jre-alpine-buildlabels">8u151-jre-alpine-buildlabels</a>
     * <pre>
     *     dockerfileSet = '8u151-jre-alpine-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/defaultdocker">defaultdocker</a>
     * <pre>
     *     dockerfileSet = 'defaultdocker'
     * </pre>
     *
     * If not specified, the {@code Dockerfile} and {@code bootrunner.sh} file from the dockerfileSet defaultdocker will be used, unless you also have files in {@link #dockerSrcDirectory}

     */
    void setDockerfileSet(String dockerfileSet){
        this.dockerfileSet(dockerfileSet)
    }

    /**
     * Instead of using the default {@code Dockerfile} and {@code bootrunner.sh}, you can specify a different
     * pre-packaged {@code Dockerfile} and {@code bootrunner.sh}:
     *
     * <br><br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/90111-jre-sid-buildlabels">90111-jre-sid-buildlabels</a>
     * <pre>
     *     dockerfileSet = '90111-jre-sid-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/8u151-jre-alpine-buildlabels">8u151-jre-alpine-buildlabels</a>
     * <pre>
     *     dockerfileSet = '8u151-jre-alpine-buildlabels'
     * </pre>
     *
     * <br>
     * To use files from <a href="https://github.com/gclayburg/dockerPreparePlugin/tree/master/src/main/resources/defaultdocker">defaultdocker</a>
     * <pre>
     *     dockerfileSet = 'defaultdocker'
     * </pre>
     *
     * If not specified, the {@code Dockerfile} and {@code bootrunner.sh} file from the dockerfileSet defaultdocker will be used, unless you also have files in {@link #dockerSrcDirectory}

     */
    void dockerfileSet(String dockerfileset){
        this.dockerfileSet = dockerfileset
    }
}

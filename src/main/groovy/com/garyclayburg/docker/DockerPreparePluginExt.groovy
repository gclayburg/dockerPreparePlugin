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
 * <br>
 * It is not necessary to provide settings for the com.garyclayburg.dockerprepare gradle plugin.  In this case, we will use this Dockerfile:
 * <pre>
 *     FROM openjdk:8u131-jre-alpine
 *
 *     RUN adduser -D -s /bin/sh springboot
 *     COPY ./bootrunner.sh /home/springboot/bootrunner.sh
 *     RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
 *     WORKDIR /home/springboot
 *     USER springboot

 *     ADD dependenciesLayer/ /home/springboot/app/
 *     ADD classesLayer/ /home/springboot/app/

 *     ARG ORG_LABEL_SCHEMA_VCS_REF
 *     ARG ORG_LABEL_SCHEMA_VCS_URL
 *     ARG ORG_LABEL_SCHEMA_BUILD_DATE
 *     ARG ORG_LABEL_SCHEMA_VERSION
 *     ARG ORG_LABEL_SCHEMA_DESCRIPTION
 *     ARG MAINTAINER
 *     LABEL maintainer=${MAINTAINER:-"https://github.com/gclayburg"} \
 *     org.label-schema.vcs-ref=${ORG_LABEL_SCHEMA_VCS_REF} \
 *     org.label-schema.vcs-url=${ORG_LABEL_SCHEMA_VCS_URL} \
 *     org.label-schema.build-date=${ORG_LABEL_SCHEMA_BUILD_DATE} \
 *     org.label-schema.version=${ORG_LABEL_SCHEMA_VERSION} \
 *     org.label-schema.schema-version="1.0" \
 *     org.label-schema.description=${ORG_LABEL_SCHEMA_DESCRIPTION}

 *     VOLUME /tmp
 *     EXPOSE 8080
 *     ENV JAVA_OPTS=""
 *     ENTRYPOINT ["./bootrunner.sh"]
 *</pre>
 * <p>
 *     And this bootrunner.sh
 *<pre>
 *     #!/bin/sh
 *     date_echo(){
 *         datestamp=$(date "+%F %T")
 *         echo "${datestamp} $*"
 *     }
 *     #exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully
 *     if [ -d "${HOME}/app" ]; then
 *       #execute springboot expanded jar, which may have been constructed from several image layers
 *       date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.JarLauncher $*"
 *       # shellcheck disable=SC2086
 *       exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.JarLauncher "$@"
 *     elif [ -f "${HOME}/app.jar" ]; then
 *       # execute springboot jar
 *       date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar ${HOME}/app.jar $*"
 *       # shellcheck disable=SC2086
 *       exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${HOME}/app.jar" "$@"
 *     else
 *       date_echo "springboot application not found in ${HOME}/app or ${HOME}/app.jar"
 *       exit 1
 *     fi
 *     </pre>

 * If you need to override these files or add others you can put this in your build.gradle
 * <pre>
 *     dockerlayer {
 *         dockerSrcDirectory "${project.rootDir}/dockerroot"
 *     }
 *     </pre>
 *     <p>
 *
 * @author Gary Clayburg
 */
@Slf4j
class DockerPreparePluginExt {
    final Project project
    String dockerSrcDirectory
    String dockerBuildDirectory
    String dockerBuildClassesDirectory
    String dockerBuildDependenciesDirectory
    String superDependenciesDirectory

    DockerPreparePluginExt(Project project) {
        this.project = project
    }

    DockerPreparePluginExt() {
    }

    /**
     * Apply settings via closure, e.g. in build.gradle:
     * <pre>
     *     dockerprepare{
     *         dockerSrcDirectory "${project.rootDir}/mydocker"
     *         dockerBuildDirectory  "${project.buildDir}/mydockerbuild"
     *     }
     *     </pre>
     *
     *     If there is no dockerprepare section in build.gradle, these defaults are used:
     *     <pre>
     *     dockerSrcDirectory = "${project.rootDir}/src/main/docker"
     *     dockerBuildDirectory = "${project.buildDir}/docker"
     *     </pre>

     * @param closure  settings to override
     * @return
     */
    def dockerprepare(Closure closure){ //method name matches config block override:
        closure.delegate = this
        closure()
    }

    void setDockerSrcDirectory(String dockerSrcDirectory) {
        this.dockerSrcDirectory(dockerSrcDirectory)
    }

    /**
     * All files or directories in this directory will be copied to the
     * dockerBuildDirectory.  If you want to use a custom Dockerfile to
     * create your image, it should be placed here.
     * If the directory does not exist or is empty, a default Dockerfile
     * and start script for your app will be placed in the
     * dockerBuildDirectory
     * <p> Defaults to "${project.buildDir}/docker"
     * @param sourceDirectory Source directory for adding files to docker
     */
    void dockerSrcDirectory(String sourceDirectory){ //called when closure is applied
        log.info("set dockerSrcDirectory to ${sourceDirectory}")
        this.dockerSrcDirectory = sourceDirectory

    }

    /**
     * This plugin will copy your application files and supporting docker files to this directory.  If needed, you could build a docker image by hand from this directory using a command like this:
     * <pre>
     * docker build -t myname/myimage .
     * </pre>
     * You could also use a gradle task to build the image directly from these files.
     * <p>Defaults to  "${project.buildDir}/docker"
     * @param dockerBuildDirectory staging area for creating a docker image
     */
    void setDockerBuildDirectory(String dockerBuildDirectory) {
        this.dockerBuildDirectory(dockerBuildDirectory)
    }

    /**
     * This plugin will copy your application files and supporting docker files to this directory.  If needed, you could build a docker image by hand from this directory using a command like this:
     * <pre>
     * docker build -t myname/myimage .
     * </pre>
     * You could also use a gradle task to build the image directly from these files.
     * <p>Defaults to  "${project.buildDir}/docker"
     * @param dockerBuildDirectory staging area for creating a docker image
     */
    void dockerBuildDirectory(String buildD){
        log.info("set dockerBuildDirectory to ${buildD}")
        this.dockerBuildDirectory = buildD
        this.dockerBuildClassesDirectory = buildD +"/classesLayer"
        this.dockerBuildDependenciesDirectory = buildD +"/dependenciesLayer"
        this.superDependenciesDirectory = buildD +"/superDependenciesLayer"
    }

}

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

import spock.lang.Specification

/**
 * <br><br>
 * Created 2017-10-14 12:38
 *
 * @author Gary Clayburg
 */
class DockerPreparePluginExtTest extends Specification {
    def "DockerSrcDirectory"() {
    }

    def "create extension"(){
        when:
        DockerPreparePluginExt extension = new DockerPreparePluginExt()
        extension.setDockerBuildDirectory("myoutputdir")
        then:

        extension.getCommonServiceDependenciesDirectory() == "myoutputdir/commonServiceDependenciesLayer1"

        when:
        extension.dockerprepare { dockerBuildDirectory = "newdir"}

        then:
        extension.getCommonServiceDependenciesDirectory() == "newdir/commonServiceDependenciesLayer1"

        when:
        extension.dockerprepare {
            commonService = ['org.springframework.boot:spring-boot-starter-web']
        }

        then:
        extension.commonService == ['org.springframework.boot:spring-boot-starter-web']

        when:
        extension.dockerprepare {
            commonService = 'invalid-format-dep'
        }

        then:
        thrown IllegalStateException

        when:
        extension.dockerprepare {
            commonService = ['spring-boot-starter-web']
        }

        then:
        thrown IllegalStateException

        when:
        extension.dockerprepare {
            commonService = ['org.springframework.boot:spring-boot-starter-actuator','org.springframework.boot:spring-boot-starter-web']
        }
        then:
        extension.commonService.contains('org.springframework.boot:spring-boot-starter-web')

    }
}

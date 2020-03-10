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

    def 'check bootRepackaged'(File inputjar,String classifier,File outputjar) {
        expect:
        DockerPreparePlugin.insertClassifier(inputjar,classifier).path == outputjar.path

        where:
        inputjar                         | classifier | outputjar
        new File('/tmp/foo.jar')         | 'boot'     | new File('/tmp/foo-boot.jar')
        new File('/tmp/foojar.jar')      | 'boot'     | new File('/tmp/foojar-boot.jar')
        new File('/tmp/foo.jar')         | 'boot-oot'| new File('/tmp/foo-boot-oot.jar')
        new File('/tmp/foo.jar')         | null       | new File('/tmp/foo.jar')
        new File('/tmp/nested/foo.jar')  | 'boot'     | new File('/tmp/nested/foo-boot.jar')
        new File('//tmp/nested/foo.jar') | 'boot'     | new File('//tmp/nested/foo-boot.jar')
        new File('/tmp/foo.war')         | 'boot'     | new File('/tmp/foo-boot.war')
        new File('/tmp/foo.bar')         | 'boot'     | new File('/tmp/foo.bar') //garbage in, garbage out
        new File('/tmp/foowar')          | 'boot'     | new File('/tmp/foowar') //garbage in, garbage out
        new File('./scanrunner/build/libs/scanrunner-0.7.8-SNAPSHOT.jar') | 'boot' | new File('./scanrunner/build/libs/scanrunner-0.7.8-SNAPSHOT-boot.jar')
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

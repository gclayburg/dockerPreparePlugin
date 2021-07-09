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
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import spock.lang.IgnoreIf

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

/**
 * <br><br>
 * Created 2017-09-04 16:49
 *
 * @author Gary Clayburg
 */
@Slf4j
class BuildScriptsGradle5PlusSpec extends SpecRoot {

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def 'spring boot 2.4.2 with snapshots'() {
        given:
        buildFile << """
//import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
    id 'org.springframework.boot' version '2.4.2'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
//    id "com.gorylenko.gradle-git-properties" version '1.5.1'
    id 'groovy'
    id 'com.garyclayburg.dockerprepare'
//    id 'com.bmuschko.docker-remote-api' version '6.4.0'
}

group = 'com.garyclayburg'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
//    mavenLocal()
}

dockerprepare {
//    commonService = ['org.springframework.boot:spring-boot-starter-web']
    snapshotLayer = true
}

dependencies {
//    implementation group: 'com.garyclayburg', name:'upbanner-starter', version: '2.1.2-SNAPSHOT'

//    implementation 'com.github.oshi:oshi-core:5.3.6'

    implementation 'org.codehaus.groovy:groovy-all:3.0.4'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
//    developmentOnly 'org.springframework.boot:spring-boot-devtools'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

//task buildImage(type: DockerBuildImage, dependsOn: 'dockerLayerPrepare') {
//    description = 'Package application as Docker image'
//    group = 'Docker'
//    inputDir = project.file(dockerprepare.dockerBuildDirectory)
//    images.add('registry:5000/' + rootProject.name +':latest')
//}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

//        def count = new File(testProjectDir.root,
//                'build/docker/commonServiceDependenciesLayer1/BOOT-INF/lib')
//                .listFiles().size()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

//        count == 30
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def 'spring boot 2.5.2 '() {
        given:
        buildFile << """
plugins {
  id 'org.springframework.boot' version '2.5.2'
  id 'io.spring.dependency-management' version '1.0.11.RELEASE'
  id 'groovy'
  id 'com.garyclayburg.dockerprepare'
}

group = 'com.garyclayburg'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

dockerprepare {
    snapshotLayer = true
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-actuator'
  implementation 'org.springframework.boot:spring-boot-starter-security'
  implementation 'org.springframework.boot:spring-boot-starter-web'
  implementation 'org.codehaus.groovy:groovy'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
  testImplementation 'org.springframework.security:spring-security-test'
}

test {
    useJUnitPlatform()
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

//        def count = new File(testProjectDir.root,
//                'build/docker/commonServiceDependenciesLayer1/BOOT-INF/lib')
//                .listFiles().size()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

//        count == 30
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def 'spring boot 2.5.2 with war'() {
        given:
        buildFile << """
plugins {
  id 'org.springframework.boot' version '2.5.2'
  id 'io.spring.dependency-management' version '1.0.11.RELEASE'
  id 'groovy'
  id 'com.garyclayburg.dockerprepare'
}

apply plugin: 'war'
group = 'com.garyclayburg'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

configurations {
  providedRuntime
}


dockerprepare {
    snapshotLayer = true
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-actuator'
  implementation 'org.springframework.boot:spring-boot-starter-security'
  implementation 'org.springframework.boot:spring-boot-starter-web'
  implementation 'org.codehaus.groovy:groovy'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
  testImplementation 'org.springframework.security:spring-security-test'
}

test {
    useJUnitPlatform()
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

//        def count = new File(testProjectDir.root,
//                'build/docker/commonServiceDependenciesLayer1/BOOT-INF/lib')
//                .listFiles().size()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

//        count == 30
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def 'spring boot 2.5.2 with buildImage'() {
        given:
        buildFile << """
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage

plugins {
  id 'org.springframework.boot' version '2.5.2'
  id 'io.spring.dependency-management' version '1.0.11.RELEASE'
  id 'groovy'
  id 'com.garyclayburg.dockerprepare'
  id 'com.bmuschko.docker-remote-api' version '6.4.0'
}

group = 'com.garyclayburg'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

dockerprepare {
    snapshotLayer = true
}

dependencies {
  implementation 'org.springframework.boot:spring-boot-starter-actuator'
  implementation 'org.springframework.boot:spring-boot-starter-security'
  implementation 'org.springframework.boot:spring-boot-starter-web'
  implementation 'org.codehaus.groovy:groovy'
  testImplementation 'org.springframework.boot:spring-boot-starter-test'
  testImplementation 'org.springframework.restdocs:spring-restdocs-mockmvc'
  testImplementation 'org.springframework.security:spring-security-test'
}

test {
    useJUnitPlatform()
}

task buildImage(type: DockerBuildImage, dependsOn: 'dockerLayerPrepare') {
    description = 'Package application as Docker image'
    group = 'Docker'
    inputDir = project.file(dockerprepare.dockerBuildDirectory)
    images.add('registry:5000/' + rootProject.name +':latest')
}

"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

//        def count = new File(testProjectDir.root,
//                'build/docker/commonServiceDependenciesLayer1/BOOT-INF/lib')
//                .listFiles().size()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

//        count == 30
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def "ok"() {
        expect:
        true
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    @IgnoreIf({ !SpecRoot.isUsingModernGradle() })
    def 'spring boot 2.4.2 with snapshots and commonservice'() {
        given:
        buildFile << """
plugins {
    id 'org.springframework.boot' version '2.4.2'
    id 'io.spring.dependency-management' version '1.0.11.RELEASE'
    id 'groovy'
    id 'com.garyclayburg.dockerprepare'
}

group = 'com.garyclayburg'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '1.8'

repositories {
    mavenCentral()
}

dockerprepare {
    commonService = ['org.springframework.boot:spring-boot-starter-web']
    snapshotLayer = true
}

dependencies {
    implementation 'org.codehaus.groovy:groovy-all:3.0.4'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

test {
    useJUnitPlatform()
}

"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build', '--stacktrace', '--info')
                .withPluginClasspath()
                .build()
        println "build output is:"
        println result.output

//        def count = new File(testProjectDir.root,
//                'build/docker/commonServiceDependenciesLayer1/BOOT-INF/lib')
//                .listFiles().size()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerLayerPrepare').outcome == SUCCESS
        result.task(':expandBootJar').outcome == SUCCESS

//        count == 30
    }

}

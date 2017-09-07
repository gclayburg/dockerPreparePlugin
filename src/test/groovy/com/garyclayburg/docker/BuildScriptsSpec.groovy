package com.garyclayburg.docker

import groovy.util.logging.Slf4j
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.gradle.testkit.runner.internal.FeatureCheckBuildResult
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

import static org.gradle.testkit.runner.TaskOutcome.*

/**
 * <br><br>
 * Created 2017-09-04 16:49
 *
 * @author Gary Clayburg
 */
@Slf4j
class BuildScriptsSpec extends Specification {
    @Rule final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    def "setup"() {
        log.info("setup spec")
        buildFile = testProjectDir.newFile('build.gradle')
        def srcdir = testProjectDir.newFolder('src', 'main', 'groovy', 'dummypackage')
        File mainclass = new File(srcdir,'SimpleMain.groovy')
        mainclass.createNewFile()
        mainclass <<"""
package dummypackage

class DockerplugindemoApplication {

	static void main(String[] args) {
	    println('this is not the main you are looking for')
	}
}

"""
    }


    def 'bootRepackage by itself works'(){
        given:
        buildFile <<"""
buildscript {
	ext {
		springBootVersion = '1.5.6.RELEASE'
	}
	repositories {
		mavenCentral()
		maven{
			url uri('/home/gclaybur/dev/repo')
		}
	}
	dependencies {
		classpath("org.springframework.boot:spring-boot-gradle-plugin:1.5.6.RELEASE")
	}
}

apply plugin: 'groovy'
apply plugin: 'eclipse'
apply plugin: 'org.springframework.boot'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}


dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('build','--stacktrace')
                .build()

        then:
        result.output.contains('BUILD SUCCESSFUL')
        result.task(':build').outcome == SUCCESS
    }

    def 'apply dockerprepare to vanilla build'(){
        given:
        buildFile <<"""
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dockerlayer{
  	dockerBuildDirectory "\${project.buildDir}/docker"
	dockerSrcDirectory "\${project.rootDir}/src/main/docker"

}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dockerPrepare','--stacktrace','--info')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerPrepare').outcome == SUCCESS
        result.task(':copyClasses').outcome == SUCCESS
    }

    def 'apply dockerprepare without explicit dockerlayer{}'(){
        given:
        buildFile <<"""
plugins {
    id 'com.garyclayburg.dockerprepare'
    id 'org.springframework.boot' version '1.5.6.RELEASE'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dependencies {
	compile('org.springframework.boot:spring-boot-starter-actuator')
	compile('org.springframework.boot:spring-boot-starter-web')
	compile('org.codehaus.groovy:groovy')
	runtime('org.springframework.boot:spring-boot-devtools')
	testCompile('org.springframework.boot:spring-boot-starter-test')
}
"""
        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dockerPrepare','--stacktrace')
                .withPluginClasspath()
                .build()

        then:
        result.output.contains('SUCCESSFUL')
        result.task(':dockerPrepare').outcome == SUCCESS
        result.task(':copyClasses').outcome == SUCCESS
    }

    def 'apply dockerprepare without springboot'(){
        given:
        buildFile <<"""
plugins {
    id 'com.garyclayburg.dockerprepare'
}

apply plugin: 'groovy'
apply plugin: 'eclipse'

version = '0.0.1-SNAPSHOT'
sourceCompatibility = 1.8

repositories {
	mavenCentral()
}

dockerlayer{
  	dockerBuildDirectory "\${project.buildDir}/docker"
	dockerSrcDirectory "\${project.rootDir}/src/main/docker"

}

dependencies {
	compile('org.codehaus.groovy:groovy')
}
"""
        when:
        GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('dockerPrepare','--info')
                .withPluginClasspath()
                .build()

        then:
        thrown UnexpectedBuildFailure
    }

}

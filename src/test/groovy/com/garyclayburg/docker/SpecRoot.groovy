/*
 * Copyright (c) 2021 Gary Clayburg
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

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.junit.rules.TestName
import spock.lang.Specification

/**
 * <br><br>
 * Created 2021-07-08 14:09
 *
 * @author Gary Clayburg
 */
class SpecRoot extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile

    @Rule
    TestName name = new TestName()
    private File srcdir

    def cleanup() {
        def root = testProjectDir.getRoot()
        println "build dir after test: ${name.methodName}"
        root.traverse {
            println "builddir ${it}"
        }
    }

    def "setup"() {
        println "running test: ${name.methodName}"
        buildFile = testProjectDir.newFile('build.gradle')
        srcdir = testProjectDir.newFolder('src', 'main', 'groovy', 'dummypackage')
        createclass('SimpleMain.groovy', """
package dummypackage

import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class DockerplugindemoApplication {

	static void main(String[] args) {
	    println('this is not the main you are looking for')
	}
}

""")
    }

    void createclass(String filename, String contents) {
        File mainclass = new File(this.srcdir, filename)
        mainclass.createNewFile()
        mainclass << contents
    }

    /*
    Some tests can ONLY succeed when running with Gradle 4.
    Some tests can ONLY succeed with running with a newer Gradle like 6 or 7.
    So This is a hack to run all the tests in a multi step procedure

$ sdk use gradle 6.8.3
$ MODERN_GRADLE=true gradle clean build --info > build683.log
$ sdk use gradle 6.8.3 && gradle --version && MODERN_GRADLE=true gradle clean build

$ sdk use gradle 4.10.3
$ gradle clean build --info > build410.log
$ sdk use gradle 4.10.3 && gradle --version && MODERN_GRADLE=false gradle clean build --info > build410.log

     */
    static boolean isUsingModernGradle() {
        Boolean.valueOf(System.getenv("MODERN_GRADLE"))
    }

}

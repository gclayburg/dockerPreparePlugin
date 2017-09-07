package com.garyclayburg.docker

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertNotNull

/**
 * <br><br>
 * Created 2017-09-04 16:06
 *
 * @author Gary Clayburg
 */
class DockerPluginTest {

    @Test
    void pluginloads() throws Exception {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply('org.springframework.boot') //bootRepackage must be loaded first
        project.pluginManager.apply('com.garyclayburg.dockerprepare')
        assertNotNull(project.tasks.copyClasses)
    }
}

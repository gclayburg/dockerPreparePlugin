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
        assertNotNull(project.tasks.expandBootJar)
    }
}

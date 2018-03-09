/*
 * Copyright (c) 2018 Gary Clayburg
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

package org.gradle.sample.services;

import org.gradle.sample.apiImpl.PersonImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <br><br>
 * Created 2018-02-13 17:42
 *
 * @author Gary Clayburg
 */
@SpringBootApplication
public class BootUp {

    @SuppressWarnings("UnusedDeclaration")
    private static final Logger log = LoggerFactory.getLogger(BootUp.class);

    public static void main(String[] args){
        ConfigurableApplicationContext context = SpringApplication.run(BootUp.class, args);
        PersonImpl testPerson = new PersonImpl("Build", "Master");
        log.info("Started: " + context.getEnvironment().getProperty("info.app.name"));
        log.info("Created: " + testPerson.getFirstname() + " " + testPerson.getSurname());
    }
}

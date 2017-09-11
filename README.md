Docker Prepare Gradle Plugin

This is a gradle plugin that you can use in your spring boot project to create simple and optimized docker images.

To use this, you first need a Spring boot application that you build with gradle.  Add this snippet to your build.gradle file:

```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "0.9"
}
```


Quickstart

Lets build a hello world style app using this plugin.

1. create a new spring boot app from http://start.spring.io
2. select `Gradle Project` from the drop down menu at the top of the page
3. Add the `Web` dependency.  You can add other dependencies if you like, but you don't need to.  The screen should look like this:
<image>

4. Click the "Generate Project" button and download the created zip file.
5. unzip this file
6. build and run the project - 

```bash
$ ./gradlew build
$ java -jar build/libs/demo-0.0.1-SNAPSHOT.jar
```

You should see this:

```bash
2017-09-08 15:43:56.135  INFO 31221 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2017-09-08 15:43:56.201  INFO 31221 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2017-09-08 15:43:56.207  INFO 31221 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 2.706 seconds (JVM running for 3.072)

```

This is just a simple spring boot application that runs an embedded Tomcat server with our simple app in it.  Everything we need is in this one jar file.  

What we want to do now is take this app and bundle it inside a docker container with the JVM and everything the JVM needs.  

Add this to your build.gradle file.  Or check out the example at `sampleapp/demo`

```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "0.9"
}
```



Now we could simply take this one jar file and run it inside a docker container much like we just did above.  One example of how to do this is here  https://spring.io/guides/gs/spring-boot-docker/

This plugin takes a little different approach.  Instead of taking this one big jar file 

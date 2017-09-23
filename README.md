Docker Prepare Gradle Plugin

This is a gradle plugin that you can use in your spring boot project to prepare the spring boot jar to run as a docker image.  This plugin goes beyond simply `ADD`ing the spring boot jar file to a `Dockerfile`.  This plugin will:

1. Create an opinionated Dockerfile that
  * uses the official [openjdk jre-alpine image](https://hub.docker.com/_/openjdk/) as a base
  * runs your app as a non-root user
  * splits your spring boot jar into 2 layers for compact builds - one for dependencies and one for classes
  * exposes port 8080
  * runs the application using a script that allows for your app to receive OS signals when running inside the container - your app will shutdown cleanly
  * passes along any runtime arguments to your application 

Note: this plugin merely populates a `build/docker` directory.  It does not create docker images or use the docker API in any way.  There are other gradle plugins for that.  [See below](#build-docker-image-with-gradle).

# Using
To use this, add this snippet to your build.gradle file:

```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "0.9.3"
}
```

And build your app:

```bash
$ ./gradlew build
$ ls build/docker
bootrunner.sh  classesLayer/  dependenciesLayer/  Dockerfile
```

To create your docker image by hand with these files:

```bash
$ cd build/docker
$ docker build -t demoapp:latest .
```

# Quickstart with a new demo app

Lets build a hello world style app using this plugin.

1. create a new spring boot app from [http://start.spring.io]
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

```text
2017-09-18 18:09:11.138  INFO 14774 --- [           main] o.s.j.e.a.AnnotationMBeanExporter        : Registering beans for JMX exposure on startup
2017-09-18 18:09:11.199  INFO 14774 --- [           main] s.b.c.e.t.TomcatEmbeddedServletContainer : Tomcat started on port(s): 8080 (http)
2017-09-18 18:09:11.205  INFO 14774 --- [           main] com.example.demo.DemoApplication         : Started DemoApplication in 2.552 seconds (JVM running for 2.921)
```
Stop it with ctrl-c.

```text
^C2017-09-18 18:09:14.436  INFO 14774 --- [       Thread-3] ationConfigEmbeddedWebApplicationContext : Closing org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@30946e09: startup date [Mon Sep 18 18:09:09 MDT 2017]; root of context hierarchy
2017-09-18 18:09:14.437  INFO 14774 --- [       Thread-3] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
```

Notice the output generated when we stopped the app.  Our ctrl-c on the terminal sent a SIGINT to our application which was able to trap this and [perform some standard spring boot cleanup action](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-spring-application.html#boot-features-application-exit).  In the default case here, a JMX bean cleaned up itself.  

This is just a simple spring boot application that runs an embedded Tomcat server with our simple app along with all its dependencies.  Everything we need is in this one jar file - well everything except the JVM to run it and supporting OS libraries.  Docker to the rescue!  

What we want to do now is take this app and bundle it inside a docker container with the JVM and everything the JVM needs.  

1. Add this to your build.gradle file.  Or check out the example at [sample/demo](sample/demo)
```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "0.9.3"
}
```
2. Now run the build again and check the `build/docker` directory
```bash
$ ./gradlew build
...
BUILD SUCCESSFUL

$ ls build/docker
bootrunner.sh  classesLayer/  dependenciesLayer/  Dockerfile
```

These files were created by this dockerprepare plugin.  Lets take a look at these files:

### [Dockerfile](src/main/docker/Dockerfile)
```dockerfile
FROM openjdk:8u131-jre-alpine
# we choose this base image because:
# 1. it is the latest Java 8 version on alpine as of September 2017
# 2. jre-alpine instead of jdk-alpine is much smaller but still enough to
#    run most microservice applications on the JVM
# 3. jre-alpine has a smaller security footprint than other official Java docker images
# 4. the explicit version number means the a build will be repeatable
#    i.e. not dependent on what version may have been pulled from a
#    docker registry before.

RUN adduser -D -s /bin/sh springboot
COPY ./bootrunner.sh /home/springboot/bootrunner.sh
RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
WORKDIR /home/springboot
USER springboot
# We add a special springboot user for running our application.
# Java applications do not need to be run as root

ADD dependenciesLayer/ /home/springboot/app/
# this layer contains dependent jar files of the app.  Most of the time,
# having dependencies in this layer will take advantage of the docker layer
# cache.  This will give you faster build times, faster image
# uploads/downloads and reduced storage requirements
ADD classesLayer/ /home/springboot/app/
# classesLayer contains your application classes.  This layer will
# likely change on each docker image build so we expect a docker cache miss

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]
```

There are two ADD commands in here.  This plugin split the Spring boot jar file into two directories - one for your applicaiton code classes and the other for dependent jar files.  This way, we get all the benefits of a spring boot runnable jar file and yet we can still use the docker layer cache

### [bootrunner.sh](src/main/docker/bootrunner.sh)
```bash
#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
#exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully
if [ -d "${HOME}/app" ]; then
  #execute springboot expanded jar, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.JarLauncher $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.JarLauncher "$@"
elif [ -f "${HOME}/app.jar" ]; then
  # execute springboot jar
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar ${HOME}/app.jar $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${HOME}/app.jar" "$@"
else
  date_echo "springboot application not found in ${HOME}/app or ${HOME}/app.jar"
  exit 1
fi 
```

If you like, you can create a docker image manually from the files in this directory:

```bash
$ cd build/docker
$ docker build -t myorg/demo:latest .
```

#Build Docker Image with Gradle

If you'd rather build the docker image automatically in a gradle build there are several gradle plugins that can do this.  An example of doing this with the `com.bmuschko.docker-remote-api` is shown in [sample/demo/build.gradle](sample/demo/build.gradle).

```bash
$ cd sample/demo
$ ./gradle buildImage
```

# Running the docker image

### run in foreground
```bash
$ docker run myorg/demo:latest
```
Stop it with ctrl-c.  Notice we still see the JMX beans unregistering:
```text
^C2017-09-23 22:24:05.963  INFO 1 --- [       Thread-3] ationConfigEmbeddedWebApplicationContext : Closing org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext@65b3120a: startup date [Sat Sep 23 22:23:59 GMT 2017]; root of context hierarchy
2017-09-23 22:24:05.965  INFO 1 --- [       Thread-3] o.s.j.e.a.AnnotationMBeanExporter        : Unregistering JMX-exposed beans on shutdown
```

### run in foreground with custom port mapping and property override:
```bash
$ docker run -p8081:8011 myorg/demo:latest --server.port=8011
```

Inside the container, the application is listening on port 8011.  Outside the container, it is listening on localhost:8081.  Note: you normally won't need to change the internal port like this since the docker container sees its own network namespace.  We are just showing how the docker packaged app can override any standard spring externalized property like `server.port`.

### run in foreground with custom java max heap:
```bash
$ docker run -p8080:8080 --env JAVA_OPTS='-Xmx500m' myorg/demo:latest
```

# Impacts to Gradle lifecycle

This plugin inserts a few gradle tasks into the normal gradle lifecycle when `com.garyclayburg.dockerprepare` is applied.  These tasks run after the spring boot 'bootRepackage' task and before `assemble`.  

Gradle lifecycle when using spring boot 1.x:

`classes` --> `jar` --> `bootRepackage` -->  **`dockerLayerPrepare`** --> `assemble` --> `build`

Gradle lifecycle when using spring boot 2.0+:

`classes` --> `jar` --> `bootJar` -->  **`dockerLayerPrepare`** --> `assemble` --> `build`


This is why our gradle `buildImage` task dependsOn `dockerLayerPrepare`:

```groovy
task buildImage(type: com.bmuschko.gradle.docker.tasks.image.DockerBuildImage, dependsOn: 'dockerLayerPrepare') {
	description = "build and tag a Docker Image"
	inputDir = project.file(project.dockerprepare.dockerBuildDirectory)
	tags = [dockerImageName+':latest',dockerImageName+':' +version]
}
```

Notice also that we are specifying an `inputDir` for `buildImage`.  `project.dockerprepare.dockerBuildDirectory` is the staging directory used by this plugin. Normally, `build/docker`.

# Customizing

### Use your own Dockerfile

If you'd rather use your own `Dockerfile`, just copy it and any other static files you need in your docker image into `src/main/docker`.  You'll need to create this directory if it does not exist in your project.  If this plugin finds any files in this directory, it will copy those into the dockerBuildDirectory instead of the default `Dockerfile` and `bootrunner.sh`.  The plugin will still create the docker layer directories so you might want to use the default [Dockerfile](src/main/docker/Dockerfile) as a guide.

### Use a custom location for Dockerfile

In most cases you won't need this, but there are 2 properties with the `dockerprepare` gradle extension that can be overridden if necessary.   For example, if you'd rather use `mydockersrc` directory instead of `src/main/docker` and a different build directory:

```groovy
dockerprepare{
  	dockerBuildDirectory "${project.buildDir}/dockerstaging"
	dockerSrcDirectory "${project.rootDir}/mydockersrc"
}
```
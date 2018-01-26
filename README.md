# Docker Prepare Gradle Plugin

[![Build Status](https://travis-ci.org/gclayburg/dockerPreparePlugin.svg?branch=master)](https://travis-ci.org/gclayburg/dockerPreparePlugin)

This is a gradle plugin that you can use in your [Spring Boot](https://projects.spring.io/spring-boot/) project to prepare the Spring Boot jar to run as a [docker](https://www.docker.com/) image.  This plugin goes beyond simply `ADD`ing the Spring Boot jar/war file to a `Dockerfile`.  This plugin will create an opinionated Dockerfile and staging directory that
  * uses the official [openjdk jre-alpine image](https://hub.docker.com/_/openjdk/) as a base
  * runs your app as a non-root user
  * splits your Spring Boot jar into several docker layers to maximize 
    * docker [cache hits](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/#build-cache) on subsequent builds of your project
    *  docker [cache hits](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/#build-cache) *between* similar, but independent projects
  * exposes port 8080
  * runs the application using a script that allows for your app to receive OS signals when running inside the container - your app will shutdown cleanly
  * passes along any runtime arguments to your application 

Note: this plugin merely populates a `build/docker` staging directory.  It does not create docker images or use the docker API in any way.  There are other gradle plugins for that.  [See below](#build-docker-image-with-gradle).

This plugin is really meant for when you have a Spring Boot application that you want to run independently in a docker container.  By default, the only process inside the container is the JVM that runs your application.  In this case, we can cleanly pass OS signals to your application.  More complex scenarios are possible [Customizing](#customizing)

# Using
To use this, add this snippet to your build.gradle file or use the example at [sample/demo](sample/demo)

```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "1.1.1"
}
```
The latest version with detailed install instructions can be found on the [gradle plugin portal](https://plugins.gradle.org/plugin/com.garyclayburg.dockerprepare)

Build your app:

```bash
$ ./gradlew build
$ ls build/docker
bootrunner.sh  classesLayer3/  commonServiceDependenciesLayer1/  dependenciesLayer2/  Dockerfile
```

Create your docker image by hand with these files:

```bash
$ cd build/docker
$ docker build -t demoapp:latest .
```

Thats it!  You now have a efficient, streamlined docker image of your Spring Boot app that you can run.  The next build you perform will be much faster since it uses the [docker cache](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/#build-cache) as much as possible. 

# What is in the layers?

This plugin runs in the gradle execution phase after Spring Boot creates your jar or war file.  It splits this application into 3 layers to maximize the chance of docker cache hits.
 
### build/docker/commonServiceDependenciesLayer1/

This layer is empty by default, but you might want to stuff common jars in here when you know you have several microservice style projects that share dependencies.

If you have multiple projects that create a war file, you might add this to your build.gradle:
```groovy
dockerprepare {
  commonService = ['org.springframework.boot:spring-boot-starter-tomcat','org.springframework.boot:spring-boot-starter-web']
}
```
- 30 jar files
- 14 MB

or if you have several groovy web projects:

```groovy
dockerprepare {
  commonService = ['org.springframework.boot:spring-boot-starter-web','org.codehaus.groovy:groovy']
}
```
- 31 jar files
- 19 MB

**Note** : This plugin does not change how dependencies are resolved by gradle.  You still need to specify dependencies using [standard gradle syntax](https://docs.gradle.org/current/userguide/artifact_dependencies_tutorial.html).  Since gradle handles the version numbers, they are ignored in the `commonService` declaration.

### build/docker/dependenciesLayer2/

This layer is created automatically by this plugin.  It contains all dependent jar files that are not transitive dependencies of a `commonService`

### build/docker/classesLayer3/

This layer is created automatically by this plugin. It contains everything else isn't in `build/docker/commonServiceDependenciesLayer1` or `build/docker/dependenciesLayer2`

# Quickstart with a new demo app

Lets build a hello world style app using this plugin.

1. create a new Spring Boot app from [http://start.spring.io]
2. select `Gradle Project` from the drop down menu at the top of the page
3. Add the `Web` dependency.  You can add other dependencies if you like, but you don't need to.  
4. Click the "Generate Project" button and download the created zip file.
5. unzip, build and run:

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

Notice the output generated when we stopped the app.  Our ctrl-c on the terminal sent a SIGINT to our application which was able to trap this and [perform some standard Spring Boot cleanup action](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-spring-application.html#boot-features-application-exit).  In the default case here, a JMX bean cleaned up after itself.  

This is just a simple Spring Boot application that runs an embedded Tomcat server with our simple app along with all its dependencies.  Everything we need is in this one jar file - well everything except the JVM to run it and supporting OS libraries.  Docker to the rescue!  

## adding the preparedocker plugin
What we want to do now is take this app and bundle it inside a docker container with the JVM and everything the JVM needs.  

1. Add this to your build.gradle file.  Or use the example at [sample/demo](sample/demo)
```groovy
plugins {
  id "com.garyclayburg.dockerprepare" version "1.1.1"
}
```
2. Now run the build again and check the `build/docker` directory
```bash
$ ./gradlew build
...
BUILD SUCCESSFUL

$ ls build/docker
bootrunner.sh  classesLayer3/  commonServiceDependenciesLayer1/  dependenciesLayer2/  Dockerfile
```

These files were created by this `dockerprepare` plugin.  Lets take a look at these files:

### [Dockerfile](src/main/resources/defaultdocker/Dockerfile)
```dockerfile
FROM openjdk:8u131-jre-alpine
# We choose this base image because:
# 1. it is the latest Java 8 version on alpine as of September 2017
# 2. jre-alpine instead of jdk-alpine is much smaller but still enough to
#    run most microservice applications on the JVM
# 3. jre-alpine has a smaller security footprint than other official Java docker images
# 4. the explicit version number means the build will be repeatable
#    i.e. not dependent on what :latest version may have been pulled from a
#    docker registry before.

RUN adduser -D -s /bin/sh springboot
COPY ./bootrunner.sh /home/springboot/bootrunner.sh
RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
WORKDIR /home/springboot
USER springboot
# We add a special springboot user for running our application.
# Java applications do not need to be run as root

ADD commonServiceDependenciesLayer1 /home/springboot/app/
# This layer is composed of all transitive dependencies of a
# commonService, e.g in your build.gradle:
#
#dockerprepare {
#  commonService = ['org.springframework.boot:spring-boot-starter-web']
#}
#
# All 30 jar files pulled in from spring-boot-starter-web are added to this layer

ADD dependenciesLayer2/ /home/springboot/app/
# This layer contains dependent jar files of the app that aren't a
# commonService.  Most of the time,
# having dependencies in this layer will take advantage of the docker build
# cache.  This will give you faster build times, faster image
# uploads/downloads and reduced storage requirements.
# This layer is computed automatically from your spring boot application

ADD classesLayer3/ /home/springboot/app/
# This layer contains your application classes.  It will
# likely change on each docker image build so we expect a docker cache miss.
# This layer is computed automatically from your spring boot application

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]
```

There are three ADD commands in here for the 3 layers produced by this plugin.  We still get all the benefits of a Spring Boot runnable jar file and yet we can still use the docker layer cache.

### [bootrunner.sh](src/main/resources/defaultdocker/bootrunner.sh)
```bash
#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
#exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully

if [ -d "${HOME}/app/WEB-INF" ]; then
  #execute springboot expanded war, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.WarLauncher $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.WarLauncher "$@"
elif [ -d "${HOME}/app" ]; then
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

This script will run your Spring Boot packaged jar or war so that OS signals can be trapped by your application when running under Docker.

If you like, you can create a docker image manually from the files in this directory:

```bash
$ cd build/docker
$ docker build -t myorg/demo:latest .
```

# Why are docker layers important?

An alternative way to build a docker image from a Spring Boot jar file is to simply take the generated Spring Boot jar file and run it directly inside the docker container.  Your `Dockerfile` will have a line that looks something like this:

```dockerfile
ADD build/libs/*.jar app.jar
```

Adding a jar like this and then running with a valid `ENTRYPOINT` command will work.  You will get all the benefits of running a Spring Boot self contained jar file in docker.  However, you can easily run into deployment performance issues in real projects once you start adding project dependencies and you need to do frequent builds and deploys.  

Most of the builds and deploys only involve changes to your project code, yet each build has everything bundled into this one jar file.  In our simple demo case, the jar file is about 14 MB.  This can easily grow into an app that is 50,60,70MB or even bigger.  This can become taxing in docker environments where each time you build the docker image, the old layers stick around on the filesystem.  When the old layer is 70+ MB, it doesn't take too many of these to create storage issues.  The same issues exist on your docker repository and any server that needs to pull your docker image.  You are also likely to run into network bandwidth issues, especially when you need push your image to a remote repository.  

## analyze layer sizes

So lets run `docker history` on the demo app we just created

```bash
$ docker history myorg/demo:latest
IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
5cc3037194a5        3 seconds ago       /bin/sh -c #(nop)  ENTRYPOINT ["./bootrunn...   0B                  
7a1e2fec4189        3 seconds ago       /bin/sh -c #(nop)  ENV JAVA_OPTS=               0B                  
4214e22518fe        3 seconds ago       /bin/sh -c #(nop)  EXPOSE 8080/tcp              0B                  
b942c798161e        3 seconds ago       /bin/sh -c #(nop)  VOLUME [/tmp]                0B                  
9735a02f869e        4 seconds ago       /bin/sh -c #(nop) ADD dir:91434ba976e061d8...   1.52kB              
84f35cdc7439        4 seconds ago       /bin/sh -c #(nop) ADD dir:0b68782be76612aa...   14.5MB              
11df191b4d46        4 seconds ago       /bin/sh -c #(nop) ADD dir:e0c8bf46ad783f50...   0B                  
37d0dc4f3956        13 days ago         /bin/sh -c #(nop)  USER [springboot]            0B                  
ae18c86c921d        13 days ago         /bin/sh -c #(nop) WORKDIR /home/springboot      0B                  
4792c1014667        13 days ago         /bin/sh -c chmod 755 /home/springboot/boot...   1.42kB              
6c3811d286dd        13 days ago         /bin/sh -c #(nop) COPY file:b9865a259ab98d...   1.42kB              
3e1faa3289e4        6 weeks ago         /bin/sh -c adduser -D -s /bin/sh springboot     4.83kB              
c4f9d77cd2a1        3 months ago        /bin/sh -c set -x  && apk add --no-cache  ...   77.5MB              
<missing>           3 months ago        /bin/sh -c #(nop)  ENV JAVA_ALPINE_VERSION...   0B                  
<missing>           3 months ago        /bin/sh -c #(nop)  ENV JAVA_VERSION=8u131       0B                  
<missing>           3 months ago        /bin/sh -c #(nop)  ENV PATH=/usr/local/sbi...   0B                  
<missing>           3 months ago        /bin/sh -c #(nop)  ENV JAVA_HOME=/usr/lib/...   0B                  
<missing>           3 months ago        /bin/sh -c {   echo '#!/bin/sh';   echo 's...   87B                 
<missing>           3 months ago        /bin/sh -c #(nop)  ENV LANG=C.UTF-8             0B                  
<missing>           3 months ago        /bin/sh -c #(nop)  CMD ["/bin/sh"]              0B                  
<missing>           3 months ago        /bin/sh -c #(nop) ADD file:4583e12bf5caec4...   3.97MB              

$ du -sk *
4	bootrunner.sh
48	classesLayer3
4	commonServiceDependenciesLayer1
14432	dependenciesLayer2
4	Dockerfile
```

This shows us the layers and sizes in our app.  Notice the 5th and 6th lines with `ADD` commands.  The top one is our application classes layer at 1KB and the next one is our dependencies at 14MB.  Heuristically, Most builds will not involve changes to the dependencies so when you perform a docker build, docker can reuse this image layer from the [cache](https://docs.docker.com/engine/userguide/eng-image/dockerfile_best-practices/#build-cache).

In lab testing, a `docker push` to a remote repository went from taking more than 3 minutes to just 5 seconds when the cache is used.

## commonService

Now lets add a `commonService` layer and rebuild:

```bash
$ cd ../..
$ vi build.gradle
```

```groovy
dockerprepare {
  commonService = ['org.springframework.boot:spring-boot-starter-web']
}
```

```bash
$ ./gradlew clean build
$ cd build/docker
$ docker build -t myorg/demo:latest .
```

Most of the jar files needed for this simple app have now moved to `build/docker/commonServicesDependenciesLayer1`

```bash
$ docker history myorg/demo:latest
IMAGE               CREATED             CREATED BY                                      SIZE                COMMENT
2345d4f13ee0        17 minutes ago      /bin/sh -c #(nop)  ENTRYPOINT ["./bootrunn...   0B                  
6c742d075d85        17 minutes ago      /bin/sh -c #(nop)  ENV JAVA_OPTS=               0B                  
a795162b55b6        17 minutes ago      /bin/sh -c #(nop)  EXPOSE 8080/tcp              0B                  
fee5a546ad92        17 minutes ago      /bin/sh -c #(nop)  VOLUME [/tmp]                0B                  
278fd3c44ed7        17 minutes ago      /bin/sh -c #(nop) ADD dir:91434ba976e061d8...   1.52kB              
8328a97e7fab        17 minutes ago      /bin/sh -c #(nop) ADD dir:701ec16546259e7e...   167kB               
46fa0f2be79d        17 minutes ago      /bin/sh -c #(nop) ADD dir:96a430d362e1aee4...   14.4MB     
...

$ du -sk *
4	bootrunner.sh
48	classesLayer3
14108	commonServiceDependenciesLayer1
336	dependenciesLayer2
4	Dockerfile
```

To see the impact of this layer, we need to [create a new project with the same commonService](#quickstart-with-a-new-demo-app)
.  Even though you are creating an entirely new application, the new docker image will only need a small amount of extra storage. One way to see this is by looking at the `docker build` output for your new image:

```bash
$ docker build -t myorg/demo-manual-service2:latest .

...
Step 7/13 : ADD commonServiceDependenciesLayer1 /home/springboot/app/
 ---> Using cache
 ---> 46fa0f2be79d
...
```

You can see this step of the docker build is re-using the same image layer `46fa0f2be79d` from our last build.

Another way to see this is with the excellent [nate/dockviz](https://github.com/justone/dockviz) tool like this:

```bash
$ docker run -it --rm -v /var/run/docker.sock:/var/run/docker.sock nate/dockviz -t
...
│                 │ │       ├─46fa0f2be79d Virtual Size: 95.8 MB
│                 │ │       │ └─8328a97e7fab Virtual Size: 96.0 MB
│                 │ │       │   ├─278fd3c44ed7 Virtual Size: 96.0 MB
│                 │ │       │   │ └─fee5a546ad92 Virtual Size: 96.0 MB
│                 │ │       │   │   └─a795162b55b6 Virtual Size: 96.0 MB
│                 │ │       │   │     └─6c742d075d85 Virtual Size: 96.0 MB
│                 │ │       │   │       └─2345d4f13ee0 Virtual Size: 96.0 MB Tags: myorg/demo:latest
│                 │ │       │   └─9d95ba4336e8 Virtual Size: 96.0 MB
│                 │ │       │     └─f68618beaacd Virtual Size: 96.0 MB
│                 │ │       │       └─8b20f5db1eb7 Virtual Size: 96.0 MB
│                 │ │       │         └─5e9b065d02a9 Virtual Size: 96.0 MB
│                 │ │       │           └─13f47ca03ecb Virtual Size: 96.0 MB Tags: myorg/demo-manual-service2:latest
...

```

What we have here is two independent projects with their own source code and dependencies, but now these projects can share read-only jars that are common to both projects.

# Build Docker Image with Gradle

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

Inside the container, the application is listening on port 8011.  Outside the container, it is listening on localhost:8081.  Note: you normally won't need to change the internal port like this since the docker container sees its own network namespace.  We are just showing how the docker packaged app can override any standard Spring externalized property like `server.port`.

### run in foreground with custom java max heap:
```bash
$ docker run -p8080:8080 --env JAVA_OPTS='-Xmx500m' myorg/demo:latest
```

# Impacts to Gradle lifecycle

This plugin inserts a few gradle tasks into the normal gradle lifecycle when `com.garyclayburg.dockerprepare` is applied.  These tasks run after the Spring Boot 'bootRepackage' task and before `assemble`.  

### Gradle lifecycle when using Spring Boot 1.x:

`classes` --> `jar` --> `bootRepackage` -->  **`dockerLayerPrepare`** --> `assemble` --> `build`

### Gradle lifecycle when using Spring Boot 2.0+:

`classes` --> `jar` --> `bootJar` -->  **`dockerLayerPrepare`** --> `assemble` --> `build`

### Gradle lifecycle when using Spring Boot 1.x and a war instead of jar:

`classes` --> `war` --> `bootRepackage` -->  **`dockerLayerPrepare`** --> `assemble` --> `build`


This is why our gradle `buildImage` task dependsOn `dockerLayerPrepare`:

```groovy
def dockerImageName = 'myorg/'+project.name
task buildImage(type: com.bmuschko.gradle.docker.tasks.image.DockerBuildImage, dependsOn: 'dockerLayerPrepare') {
	description = "build and tag a Docker Image"
	inputDir = project.file(project.dockerprepare.dockerBuildDirectory)
	tags = [dockerImageName+':latest',dockerImageName+':' +version]
}
```

Notice also that we are specifying an `inputDir` for `buildImage`.  `project.dockerprepare.dockerBuildDirectory` is the staging directory used by this plugin. Normally, `build/docker`.

# Customizing

### Use your own Dockerfile

If you'd rather use your own `Dockerfile`, just copy it and any other static files you need in your docker image into `src/main/docker`.  You'll need to create this directory if it does not exist in your project.  If this plugin finds any files in this directory, it will copy those into the dockerBuildDirectory instead of the default `Dockerfile` and `bootrunner.sh`.  The plugin will still create the docker layer directories in `build/docker/` so you might want to use the default [Dockerfile](src/main/resources/defaultdocker/Dockerfile) as a guide.

### Use a custom location for Dockerfile

In most cases you won't need this, but there are 2 properties with the `dockerprepare` gradle extension that can be overridden if necessary.   For example, if you'd rather use `mydockersrc` directory instead of `src/main/docker` and a different build directory:

```groovy
dockerprepare{
  	dockerBuildDirectory "${project.buildDir}/dockerstaging"
	dockerSrcDirectory "${project.rootDir}/mydockersrc"
}
```

Check out [Memuser](https://github.com/gclayburg/memuser) for an example of a customized project

# Errors

### dockerprepare cannot prepare war file that is executable

Your build.gradle is building a spring executable war file.  This causes issues for many tools that attempt to extract a jar/war/zip file.  To fix it, remove executable support:

```groovy
springBoot{
  executable = false
}

``` 

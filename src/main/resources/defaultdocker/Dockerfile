FROM openjdk:8u151-jre-alpine
# We choose this base image because:
# 1. it is the latest Java 8 version on alpine as of March 2018
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
ENV JAVA_OPTS="" \
    SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENTRYPOINT ["./bootrunner.sh"]

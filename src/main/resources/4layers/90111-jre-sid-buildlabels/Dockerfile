FROM openjdk:9.0.1-11-jre-sid
# We choose this base image because:
# 1. it is the latest Java 9 version on alpine as of March 2018

RUN adduser --shell /bin/bash --home /home/springboot --gecos springboot --disabled-password springboot
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

ADD snapshotLayer3/ /home/springboot/app/
# This layer contains artifacts labeled with *-SNAPSHOT.jar
# Since they are snapshots, they will likely change more often
# than regular dependencies in layer 2, but probably not as much as
# regular classes and resources of layer4

ADD classesLayer4/ /home/springboot/app/
# This layer contains your application classes.  It will
# likely change on each docker image build so we expect a docker cache miss.
# This layer is computed automatically from your spring boot application


ARG ORG_LABEL_SCHEMA_VCS_REF
ARG ORG_LABEL_SCHEMA_VCS_URL
ARG ORG_LABEL_SCHEMA_BUILD_DATE
ARG ORG_LABEL_SCHEMA_VERSION
ARG ORG_LABEL_SCHEMA_DESCRIPTION
ARG MAINTAINER
LABEL maintainer=${MAINTAINER:-"NA"} \
      org.label-schema.vcs-ref=${ORG_LABEL_SCHEMA_VCS_REF} \
      org.label-schema.vcs-url=${ORG_LABEL_SCHEMA_VCS_URL} \
      org.label-schema.build-date=${ORG_LABEL_SCHEMA_BUILD_DATE} \
      org.label-schema.version=${ORG_LABEL_SCHEMA_VERSION} \
      org.label-schema.description=${ORG_LABEL_SCHEMA_DESCRIPTION}

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS="" \
    SPRING_OUTPUT_ANSI_ENABLED=ALWAYS
ENTRYPOINT ["./bootrunner.sh"]

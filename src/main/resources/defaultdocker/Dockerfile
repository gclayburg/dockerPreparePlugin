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
# having dependencies in this layer will take advantage of the docker build
# cache.  This will give you faster build times, faster image
# uploads/downloads and reduced storage requirements
ADD classesLayer/ /home/springboot/app/
# classesLayer contains your application classes.  This layer will
# likely change on each docker image build so we expect a docker cache miss

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]

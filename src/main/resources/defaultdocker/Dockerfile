FROM openjdk:8u131-jre-alpine

RUN adduser -D -s /bin/sh springboot
COPY ./bootrunner.sh /home/springboot/bootrunner.sh
RUN chmod 755 /home/springboot/bootrunner.sh && chown springboot:springboot /home/springboot/bootrunner.sh
WORKDIR /home/springboot
USER springboot

ADD dependenciesLayer/ /home/springboot/app/
ADD classesLayer/ /home/springboot/app/

ARG ORG_LABEL_SCHEMA_VCS_REF
ARG ORG_LABEL_SCHEMA_VCS_URL
ARG ORG_LABEL_SCHEMA_BUILD_DATE
ARG ORG_LABEL_SCHEMA_VERSION
ARG ORG_LABEL_SCHEMA_DESCRIPTION
ARG MAINTAINER
LABEL maintainer=${MAINTAINER:-"https://github.com/gclayburg"} \
      org.label-schema.vcs-ref=${ORG_LABEL_SCHEMA_VCS_REF} \
      org.label-schema.vcs-url=${ORG_LABEL_SCHEMA_VCS_URL} \
      org.label-schema.build-date=${ORG_LABEL_SCHEMA_BUILD_DATE} \
      org.label-schema.version=${ORG_LABEL_SCHEMA_VERSION} \
      org.label-schema.schema-version="1.0" \
      org.label-schema.description=${ORG_LABEL_SCHEMA_DESCRIPTION}

VOLUME /tmp
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["./bootrunner.sh"]

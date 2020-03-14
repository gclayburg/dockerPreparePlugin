#!/bin/sh
date_echo(){
    datestamp=$(date "+%F %T")
    echo "${datestamp} $*"
}
date_echo "The application will start in ${JHIPSTER_SLEEP}s..."
sleep ${JHIPSTER_SLEEP}
if [ ! -z "${LAUNCH_DEBUG}" ]; then
  date_echo "Running with env:"
  env
fi
#exec the JVM so that it will get a SIGTERM signal and the app can shutdown gracefully
if [ -f "${HOME}/*.jar" ]; then
  # execute springboot jar
  JARFILE=$(ls ${HOME}/*jar)
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar ${JARFILE} $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${JARFILE}" "$@"
elif [ -f ${HOME}/*.war ]; then
  # execute springboot war
  WARFILE=$(ls ${HOME}/*war)
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar ${WARFILE} $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -jar "${WARFILE}" "$@"
elif [ -d "${HOME}/app/WEB-INF" ]; then
  #execute springboot expanded war, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.WarLauncher $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.WarLauncher "$@"
elif [ -d "${HOME}/app" ]; then
  date_echo "execute app"
  #execute springboot expanded jar, which may have been constructed from several image layers
  date_echo "exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp ${HOME}/app org.springframework.boot.loader.JarLauncher $*"
  # shellcheck disable=SC2086
  exec java ${JAVA_OPTS} -Djava.security.egd=file:/dev/./urandom -cp "${HOME}/app" org.springframework.boot.loader.JarLauncher "$@"
else
  date_echo "springboot application not found in ${HOME}/app, ${HOME}/app.jar, or ${HOME}/app.war"
  exit 1
fi

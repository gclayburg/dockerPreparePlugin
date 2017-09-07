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

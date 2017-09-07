#!groovy
def starttime = System.currentTimeMillis()
stage "provision build node"
node('coreosnode') {  //this node label must match jenkins slave with nodejs installed
    println("begin: build node ready in ${(System.currentTimeMillis() - starttime) / 1000}  seconds")
    wrap([$class: 'TimestamperBuildWrapper']) {  //wrap each Jenkins job console output line with timestamp
        stage "build setup"
        checkout scm
        whereami()

        stage "build/test"
        sh "./gradlew --no-daemon clean build"
        stage "archive"
        step([$class: 'JUnitResultArchiver', testResults: 'build/**/TEST-*.xml'])
        println "flow complete!"
    }
}
private void whereami() {
    /**
     * Runs a bunch of tools that we assume are installed on this node
     */
    echo "Build is running with these settings:"
    sh "pwd"
    sh "ls -la"
    sh "echo path is \$PATH"
    sh """
uname -a
java -version
mvn -v
docker ps
docker info
docker-compose version
npm version
gulp --version
bower --version
"""
}

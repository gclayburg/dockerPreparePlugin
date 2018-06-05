
### compile and build docker image:

```bash
./gradlew clean buildDocker
```

and run it

```bash
$ docker run  yourappname:latest
```

or run it after a 2 second delay and show runtime environment:

```bash
$ docker run --env JHIPSTER_SLEEP=2 --env LAUNCH_DEBUG=true  yourappname:latest
```

### Running with maven

In addition to building your gradle project as shown here, the 3 files in `sample-jhipster/src/main/docker/` 
can also be used verbatim in a maven based build that uses
`src/main/docker/` as the docker context.


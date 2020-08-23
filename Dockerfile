FROM maven as build
RUN useradd builder
WORKDIR /build
RUN chown builder:builder /build
USER builder
COPY --chown=builder ./ ./

RUN mvn install -Dgpg.skip -f external/oj-judge/pom.xml
RUN mvn package
RUN mkdir -p out
RUN mvn help:evaluate -q -Dexpression=project.version -DforceStdout > out/version
RUN mv target/oj-c99runner-$(cat out/version)-jar-with-dependencies.jar out/oj-c99runner.jar

FROM openjdk
RUN yum install gcc -y && yum clean all
COPY --from=build /build/out /oj-c99runner

RUN useradd -r oj-c99runner
USER oj-c99runner
EXPOSE 1993

ENV RUNNER_ADDRESS 0.0.0.0
ENV RUNNER_PORT 1993

CMD java -jar /oj-c99runner/oj-c99runner.jar

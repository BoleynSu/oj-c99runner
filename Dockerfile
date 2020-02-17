FROM openjdk:latest
RUN curl -L boleyn.su/pgp | gpg --import
RUN yum install wget gcc -y && yum clean all

ENV APPROOT=/boleyn.su/opt/boleyn.su/oj-c99runner/
RUN mkdir -p $APPROOT
WORKDIR $APPROOT

ENV VERSION=1.0.1
RUN wget https://repo1.maven.org/maven2/su/boleyn/oj/oj-c99runner/$VERSION/oj-c99runner-$VERSION-jar-with-dependencies.jar{,.asc}
RUN gpg --verify oj-c99runner-$VERSION-jar-with-dependencies.jar.asc

RUN useradd -r oj-c99runner
USER oj-c99runner:oj-c99runner
EXPOSE 1993

ENV RUNNER_ADDRESS 0.0.0.0
ENV RUNNER_PORT 1993

CMD /usr/bin/bash -c '\
    java $CONFIG -jar $APPROOT/oj-c99runner-$VERSION-jar-with-dependencies.jar'

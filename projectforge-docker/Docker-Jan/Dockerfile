FROM openjdk:11
ARG PF_VERSION
ARG PF_HOME

MAINTAINER Micromata

ADD $PF_HOME /home/pf/ProjectForge
COPY projectforge-application-$PF_VERSION.jar /home/pf/projectforge-application.jar

WORKDIR /home/pf

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "projectforge-application.jar"]
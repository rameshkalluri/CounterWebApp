

FROM maven:3.5.2-jdk-8-alpine AS MAVEN_TOOL_CHAIN
COPY pom.xml /tmp/
COPY src /tmp/src/
WORKDIR /tmp/
RUN mvn package


FROM tomcat:8.0
MAINTAINER reddy
COPY target/CounterWebApp.war /usr/local/tomcat/webapps/

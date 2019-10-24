FROM tomcat:8.0
MAINTAINER reddy
COPY target/CounterWebApp.war /usr/local/tomcat/webapps/

FROM tomcat:8.0
MAINTAINER ramesh
COPY **/**.war /usr/local/tomcat/webapps/

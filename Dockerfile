# syntax=docker/dockerfile:1
#
# Builds the example and runs one cluster node: Tomcat 9 on Java 8, keeping HTTP sessions in Redis
# through the Redisson session manager.
#
#   STRUTS=apache     Apache Struts 1.1 from Maven Central (the default)
#   STRUTS=nes-trial  the NES for Apache Struts 1.1 trial from the HeroDevs NES registry
#
# The WAR is deployed twice. /after/ is the app as built. /before/ is the same app with
# processorClass removed from struts-config.xml, so it runs Struts' default request processor.
FROM maven:3.9-eclipse-temurin-8 AS build
WORKDIR /src
COPY pom.xml settings.xml ./
COPY src ./src
ARG STRUTS=apache
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -s settings.xml -P "$STRUTS" package

FROM tomcat:9.0-jdk8-temurin
ARG REDISSON_VERSION=3.52.0
ADD https://repo1.maven.org/maven2/org/redisson/redisson-all/${REDISSON_VERSION}/redisson-all-${REDISSON_VERSION}.jar /usr/local/tomcat/lib/
ADD https://repo1.maven.org/maven2/org/redisson/redisson-tomcat-9/${REDISSON_VERSION}/redisson-tomcat-9-${REDISSON_VERSION}.jar /usr/local/tomcat/lib/
COPY docker/context.xml docker/redisson.yaml /usr/local/tomcat/conf/
COPY --from=build /src/target/session-uploads/ /usr/local/tomcat/webapps/after/
RUN chmod 644 /usr/local/tomcat/lib/redisson-*.jar \
 && cd /usr/local/tomcat/webapps \
 && cp -r after before \
 && sed -i 's| processorClass="[^"]*"||' before/WEB-INF/struts-config.xml \
 && ! grep -q processorClass before/WEB-INF/struts-config.xml \
 && mkdir ROOT && echo '<% response.sendRedirect("/before/"); %>' > ROOT/index.jsp

FROM eclipse-temurin:21-jdk-noble AS build

WORKDIR /build
COPY lib/jakarta.servlet-api-6.0.0.jar /build/lib/jakarta.servlet-api-6.0.0.jar
COPY lib/mysql-connector-j-8.4.0.jar /build/lib/mysql-connector-j-8.4.0.jar
COPY src/com /build/src/com
COPY webapp /build/webapp

RUN mkdir -p /build/classes /build/stage/greenfields/WEB-INF/classes /build/stage/greenfields/WEB-INF/lib \
    && find /build/src/com -name '*.java' ! -path '*/test/*' -print > /build/sources.txt \
    && javac --release 21 -cp /build/lib/jakarta.servlet-api-6.0.0.jar \
        -d /build/classes @/build/sources.txt \
    && cp -R /build/webapp/. /build/stage/greenfields/ \
    && cp -R /build/classes/. /build/stage/greenfields/WEB-INF/classes/ \
    && cp /build/lib/mysql-connector-j-8.4.0.jar /build/stage/greenfields/WEB-INF/lib/ \
    && jar --create --file /build/greenfields.war -C /build/stage/greenfields .

FROM tomcat:10.1.60-jre21-temurin-noble

ENV CATALINA_OPTS="-Djava.security.egd=file:/dev/./urandom"
COPY --from=build /build/greenfields.war /usr/local/tomcat/webapps/greenfields.war
COPY docker/railway-entrypoint.sh /usr/local/bin/railway-entrypoint.sh
RUN chmod 0755 /usr/local/bin/railway-entrypoint.sh

EXPOSE 8080
CMD ["/usr/local/bin/railway-entrypoint.sh"]

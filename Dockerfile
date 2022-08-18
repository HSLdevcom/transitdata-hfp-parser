FROM eclipse-temurin:11-alpine
#Install curl for health check
RUN apk add --no-cache curl

ADD target/transitdata-hfp-parser-jar-with-dependencies.jar /usr/app/transitdata-hfp-parser.jar
COPY start-application.sh /
RUN chmod +x /start-application.sh
CMD ["/start-application.sh"]

FROM eclipse-temurin:17-jdk
WORKDIR /app
COPY ChatApp.java .
RUN javac ChatApp.java && mkdir -p /app/logs
ENTRYPOINT ["java", "-cp", "/app", "ChatApp"]
CMD ["server"]

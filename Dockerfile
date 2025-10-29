FROM eclipse-temurin:21-jdk
EXPOSE 8080
ADD target/bulktransactionservice-0.0.1-SNAPSHOT.jar bulktransactionservice.jar
ENTRYPOINT ["java","-jar","/bulktransactionservice.jar"]
# ================================================================
# Dockerfile - CORBA PDF Service
# ================================================================

FROM maven:3-amazoncorretto-8

WORKDIR /app

RUN yum install -y fontconfig dejavu-sans-fonts freetype curl && yum clean all

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
COPY idl ./idl

RUN mvn clean package -DskipTests -B

ENV JAVA_OPTS="-Xms256m -Xmx512m -Djava.awt.headless=true"
ENV SERVER_PORT=8080

EXPOSE 8080
EXPOSE 1050

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar target/corba-pdf-service-1.0.0.jar"]
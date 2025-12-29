# Build stage
FROM openjdk:25-ea-jdk-oraclelinux9 AS build
WORKDIR /app

# Install utilities needed for Maven Wrapper
RUN dnf install -y tar gzip

# Copy Maven Wrapper files and Project files
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
COPY src ./src

# Ensure mvnw is executable and convert line endings (Windows to Unix)
RUN chmod +x mvnw && sed -i 's/\r$//' mvnw

# Build using the Maven Wrapper (uses the JDK 25 from the base image)
RUN ./mvnw clean package -DskipTests

# Run stage
FROM openjdk:25-ea-jdk-oraclelinux9
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]

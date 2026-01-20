# TrainTogether

TrainTogether is a software project for the OTH Regensburg.

## Local Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/enrixx/TrainTogether.git
   cd TrainTogether
   ```
2. Copy `example.env` to `.env` in the project root:
    ```bash
   cp example.env .env
    ```

3. Build with Maven:
   ```bash
    # Ohne Tests (wie in README)
    mvn clean package -DskipTests
    # Mit Tests
    mvn clean package
    ```

4. Start the needed Docker container:
   ```bash
   # for development (recommended for this methode)
    docker compose -f docker-compose.dev.yaml up -d
   # for production
    docker compose -f docker-compose.yaml up -d
    ```

## Direct Container pull and run

To pull the latest Docker image from Docker Hub:

   ```bash
   docker pull eschreider/traintogether:latest
   ```
   
   To run the application using this image:
   
   ```bash
   docker run -p 8080:8080 eschreider/traintogether:latest
   ```

# TrainTogether
TrainTogether is a software project for the OTH Regensburg. 

## Local Development Setup

1. Copy `example.env` to `.env` in the project root:

   ```bash
   cp example.env .env
   ```

2. Fill in the required variables/secrets in `.env`.

3. Start the database container:

   ```bash
  docker-compose -f docker-compose.dev.yaml up -d
   ```

4. Run the Spring Boot application locally. It will read the `.env` file automatically.

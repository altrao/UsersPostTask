# Aggregating Service: Post and User Details

## Objective
The goal of this task is to assess your ability to integrate with an external API, design a database schema, and implement a mechanism for handling data expiration and revalidation upon client request.

## Task Description
You are tasked with building a REST API using Kotlin and Spring Boot that retrieves information about a specific post by its ID. Each post should also include basic information about its author (user). The service must cache the post data in a database and only re-fetch the data from the external API if it is not present in the database or has expired based on a specified timeout.

## Requirements

### API Endpoint
- **`GET /api/posts/{id}`**
    - Fetches a post by its ID from the external API (`https://jsonplaceholder.typicode.com/posts/{id}`).
    - Includes the following fields about the author (`https://jsonplaceholder.typicode.com/users/{userId}`):
        - `id`
        - `name`
        - `username`
        - `email`
    - If the post is present in the database and valid (not expired), return it from the database.
    - If the post is not present or has expired, fetch fresh data from the external API, update the database, and return the response to the client.

### Data Expiration
- Implement a mechanism to determine if cached data is expired based on a configurable timeout (e.g., 1 hour).
- The expiration check and data refresh must only occur when the client requests the data (e.g., via `GET /api/posts/{id}`). **No background jobs or scheduled tasks are required.**

### Database Design
- Design a schema to store post and user data.
- The schema must support the described functionality, but the implementation details are entirely up to you.

### Testing
- Write at least one unit test for the service logic.
- Write at least one integration test for the endpoint.

## Expectations

1. **Database Design**
    - You must design and implement the database schema to support the described functionality.

2. **Code Quality**
    - Ensure the code is clean, readable, and well-documented.
    - Include error handling for external API failures and invalid input.

3. **Deliverable**
    - A GitHub repository containing:
        - The full source code.
        - A `docker-compose.yml` file for running a local PostgreSQL instance.
        - A `README.md` file with clear instructions on how to set up and run the project.

## Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/altrao/UsersPostTask.git
   cd UsersPostTask
   ```
2.	Run PostgreSQL using Docker: 
``` docker-compose up -d ```
3.	Configure the application:
      -	Update the application.yaml file and set the `external.json-placeholder.expiration` time in seconds
4. Run the application:
   ``` ./gradlew bootRun ```
5. Test the endpoint:
   ```GET http://localhost:8080/api/posts/1```

## Functionalities
- Posts and Users will be cached in the database, the expiration time is defined in the `application.yaml`.
- Upon requesting a post the database will be checked first and return if that post is still valid.
- Expired posts will be requested again from the source and updated in the database with a new expiration time.
- The service should be able to handle hundreds of requests per second and that can be validated using Grafana k6. Check `test.js` and proceed to https://grafana.com/docs/k6/latest/get-started/running-k6 on how to execute it through Docker. 
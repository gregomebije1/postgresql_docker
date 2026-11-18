
# Useful Commands

    ./mvnw clean compile



----
./mvnw clean package -DskipTests
----

./mvnw test
./mvnw spring-boot:run
./mvnw spring-boot:run -DskipTests

./mvnw clean test-compile
---

- ./mvnw test
- ./mvnw test -Dtest='!LedgerSystemIntegrationTest'        # unit tests only, no Docker
- ./mvnw test -Dtest=LedgerSystemIntegrationTest           # integration test only



docker compose up -d 
docker compose down -v
docker exec -it multi_db_postgres psql -U postgres
\c db_one
\df+


docker exec -it multi_db_postgres psql -U postgres -d db_one

docker exec -it multi_db_postgres psql -U postgres -d db_one -c "\dt"

docker exec -it multi_db_postgres psql -U postgres -d db_one -c "SELECT * FROM users;"

psql -h localhost -p 5432 -U postgres -d db_one
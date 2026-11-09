
# Useful Commands

docker compose up -d 
docker compose down -v
docker exec -it multi_db_postgres psql -U postgres
\c db_one

docker exec -it multi_db_postgres psql -U postgres -d db_one

docker exec -it multi_db_postgres psql -U postgres -d db_one -c "\dt"

docker exec -it multi_db_postgres psql -U postgres -d db_one -c "SELECT * FROM users;"

psql -h localhost -p 5432 -U postgres -d db_one
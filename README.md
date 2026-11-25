
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


=====
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;

mvn dependency:tree

=====
Spring Boot and Spring projects
Dependency	Documentation
Spring Boot 4.1.x (reference)	https://docs.spring.io/spring-boot/
Boot 4 migration guide and release notes	https://github.com/spring-projects/spring-boot/wiki
Boot managed dependency versions	https://docs.spring.io/spring-boot/appendix/dependency-versions/index.html
Boot Maven plugin	https://docs.spring.io/spring-boot/maven-plugin/
starter-data-jpa	https://docs.spring.io/spring-data/jpa/reference/
starter-webmvc, restclient, validation (Spring Framework)	https://docs.spring.io/spring-framework/reference/
starter-security	https://docs.spring.io/spring-security/reference/
starter-data-redis	https://docs.spring.io/spring-data/redis/reference/
Testcontainers support in Boot	https://docs.spring.io/spring-boot/reference/testing/testcontainers.html

Because you’re on Boot 4, the migration wiki is worth reading in full. Several starters were renamed (webmvc, flyway, restclient, the -test modules), and a lot of older blog posts and Stack Overflow answers target Boot 2/3 and will mislead you.

Persistence and database
Dependency	Documentation
Hibernate ORM (user guide, @Generated, @ColumnDefault, etc.)	https://hibernate.org/orm/documentation/
Jakarta Persistence spec	https://jakarta.ee/specifications/persistence/
Hibernate Validator	https://hibernate.org/validator/documentation/
Jakarta Bean Validation	https://beanvalidation.org/
Flyway	https://documentation.red-gate.com/flyway
Boot’s Flyway/DB init how-to	https://docs.spring.io/spring-boot/how-to/data-initialization.html
PostgreSQL server	https://www.postgresql.org/docs/
PostgreSQL JDBC driver	https://jdbc.postgresql.org/documentation/
H2	https://h2database.com/html/main.html
Redis	https://redis.io/docs/
Other libraries
Dependency	Documentation
JJWT	https://github.com/jwtk/jjwt
Commons Codec	https://commons.apache.org/proper/commons-codec/
Lombok	https://projectlombok.org/features/
Testcontainers	https://java.testcontainers.org/
Testcontainers PostgreSQL module	https://java.testcontainers.org/modules/databases/postgres/

JJWT 0.11.5 is old. Version 0.12+ changed the builder and parser API, so check which version’s README you’re reading. Spring Security also has built-in JWT support (spring-security-oauth2-resource-server with Nimbus), which you may prefer.

Java 21
Resource	URL
Java SE 21 documentation hub	https://docs.oracle.com/en/java/javase/21/
Java 21 API Javadoc	https://docs.oracle.com/en/java/javase/21/docs/api/
Language Specification (JLS 21)	https://docs.oracle.com/javase/specs/jls/se21/html/index.html
JVM Specification	https://docs.oracle.com/javase/specs/jvms/se21/html/index.html
OpenJDK 21 features (JEP list)	https://openjdk.org/projects/jdk/21/
Dev.java tutorials and guides	https://dev.java/learn/
Build tooling
Maven guides: https://maven.apache.org/guides/
POM reference: https://maven.apache.org/pom.html
Find artifact versions: https://central.sonatype.com/


Reference: https://docs.spring.io/spring-boot/reference/features/logging.html

Documentation for Redis, Kafka, Kubernetes and Git

As before, I’m giving these from memory. If a deep link moved, start from the home page.

Redis

Topic	URL
Redis docs (commands, data types, persistence, clustering)	https://redis.io/docs/
Redis command reference	https://redis.io/docs/latest/commands/
Spring Data Redis (what you use in code)	https://docs.spring.io/spring-data/redis/reference/
Lettuce (default Redis client in Spring Boot)	https://redis.github.io/lettuce/
Boot’s Redis properties (spring.data.redis.*)	https://docs.spring.io/spring-boot/appendix/application-properties/index.html

Kafka

Topic	URL
Apache Kafka documentation	https://kafka.apache.org/documentation/
Spring for Apache Kafka (what you’d use in code)	https://docs.spring.io/spring-kafka/reference/
Boot’s Kafka support	https://docs.spring.io/spring-boot/reference/messaging/kafka.html
Confluent Developer (tutorials and courses, free)	https://developer.confluent.io/
Testcontainers Kafka module	https://java.testcontainers.org/modules/kafka/

Since you’re using the outbox pattern, also look up Debezium (https://debezium.io/documentation/) for change-data-capture, which is a common way to ship outbox rows to Kafka.

Kubernetes

Topic	URL
Kubernetes docs (concepts, tasks, tutorials)	https://kubernetes.io/docs/home/
kubectl reference and cheat sheet	https://kubernetes.io/docs/reference/kubectl/
Spring Boot on Kubernetes (probes, graceful shutdown)	https://docs.spring.io/spring-boot/reference/deployment/cloud.html
Boot Actuator (health endpoints used by probes)	https://docs.spring.io/spring-boot/reference/actuator/
Local practice cluster: minikube	https://minikube.sigs.k8s.io/docs/
Local practice cluster: kind	https://kind.sigs.k8s.io/

To run on Kubernetes you’ll also need container basics. Docker docs are at https://docs.docker.com/, and Boot’s container image guidance is at https://docs.spring.io/spring-boot/reference/packaging/container-images/index.html.

Git

Topic	URL
Official Git documentation	https://git-scm.com/doc
Pro Git book (free, the best single resource)	https://git-scm.com/book/en/v2
Command reference	https://git-scm.com/docs
GitHub Docs (if you use GitHub)	https://docs.github.com/
Interactive branching practice	https://learngitbranching.js.org/

Offline, git help <command> (for example git help rebase) opens the same man pages.


Spring Cloud
Topic	URL
Spring Cloud project home (all sub-projects, release trains)	https://spring.io/projects/spring-cloud
Spring Cloud reference docs	https://docs.spring.io/spring-cloud/docs/current/reference/html/
Spring Cloud Gateway (relevant to your gateway project)	https://docs.spring.io/spring-cloud-gateway/reference/
Spring Cloud OpenFeign (declarative HTTP clients)	https://docs.spring.io/spring-cloud-openfeign/reference/
Spring Cloud Config (centralized configuration)	https://docs.spring.io/spring-cloud-config/reference/
Spring Cloud Netflix Eureka (service discovery)	https://docs.spring.io/spring-cloud-netflix/reference/
Spring Cloud Kubernetes (discovery and config on k8s)	https://docs.spring.io/spring-cloud-kubernetes/reference/
Spring Cloud Stream (Kafka/RabbitMQ bindings)	https://docs.spring.cloud/spring-cloud-stream/reference/
Spring Cloud Circuit Breaker (Resilience4j)	https://docs.spring.io/spring-cloud-circuitbreaker/reference/
Spring Cloud Contract (consumer-driven contract tests)	https://docs.spring.io/spring-cloud-contract/reference/



Resilience and observability (used in most microservice setups)
Topic	URL
Resilience4j (circuit breaker, retry, rate limiter, bulkhead)	https://resilience4j.readme.io/docs
Micrometer (metrics and tracing facade)	https://docs.micrometer.io/micrometer/reference/
Boot Actuator	https://docs.spring.io/spring-boot/reference/actuator/
Boot observability (metrics, tracing)	https://docs.spring.io/spring-boot/reference/actuator/observability.html
OpenTelemetry	https://opentelemetry.io/docs/
Prometheus	https://prometheus.io/docs/
Grafana	https://grafana.com/docs/


Microservice architecture and patterns (concepts, not Spring-specific)
Topic	URL
Microservices patterns catalog (Chris Richardson)	https://microservices.io/patterns/
Transactional outbox pattern (what you’re building)	https://microservices.io/patterns/data/transactional-outbox.html
Saga pattern	https://microservices.io/patterns/data/saga.html
Martin Fowler’s microservices article	https://martinfowler.com/articles/microservices.html
Azure Architecture Center (cloud design patterns)	https://learn.microsoft.com/en-us/azure/architecture/patterns/
AWS Prescriptive Guidance (microservice patterns)	https://docs.aws.amazon.com/prescriptive-guidance/latest/modernization-decomposing-monoliths/
The Twelve-Factor App	https://12factor.net/

Books worth owning
Building Microservices (Sam Newman, 2nd ed.): the standard general reference.
Microservices Patterns (Chris Richardson): covers outbox, saga, and CQRS in depth, and matches your idempotency and outbox work closely.
Designing Data-Intensive Applications (Martin Kleppmann): the best resource on consistency, messaging, and failure modes.
Spring Microservices in Action (John Carnell): Spring-specific, but check the edition, as older ones target Boot 2
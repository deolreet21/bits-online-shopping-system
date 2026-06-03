# application.properties

**File:** `src/main/resources/application.properties`  
**Owner:** Mehwish  
**Type:** Spring Boot configuration file  
**Purpose:** Central configuration for the application — database connection, JPA/Hibernate behavior, Thymeleaf rendering, and email SMTP. Read by Spring Boot on startup.

---

## Server

```properties
server.port=8080
```

The embedded Tomcat listens on port 8080. Change this to run on a different port (e.g., `server.port=9090`). Default is 8080 if omitted.

---

## DataSource (Database Connection)

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

**`${DB_URL}`** — Spring reads environment variables at startup. The actual value comes from the `.env` file (loaded by `start.sh` via `source .env`) or from the OS environment. This prevents hardcoding credentials in source code.

**`DB_URL` format:**
```
jdbc:mysql://localhost:3306/shopping_system?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

**`com.mysql.cj.jdbc.Driver`** — the MySQL Connector/J driver class. `cj` = Connector/J (the modern driver). The old `com.mysql.jdbc.Driver` is deprecated.

**Why environment variables?** Credentials in `.env` are gitignored — they never appear in version control. Teammates each have their own `.env` with their local MySQL password.

---

## JPA / Hibernate

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true
```

### `ddl-auto=update`
Hibernate automatically **updates** the database schema to match the `@Entity` classes on every startup.

| Value | Behavior |
|-------|----------|
| `create` | Drop and recreate tables on every start (data lost) |
| `create-drop` | Like create, also drops on shutdown |
| `update` | Add missing columns/tables, never drops (safe for dev) |
| `validate` | Checks schema matches entities, throws if mismatch |
| `none` | Do nothing (production default) |

**Why `update` for this project?** Development convenience — adding a new field to an entity automatically adds the column without writing a migration script. Risk: if you rename a field, the old column stays and a new column appears (data not migrated).

### `show-sql=true`
Every SQL statement Hibernate executes is printed to the console. Useful for debugging N+1 queries and verifying JOIN FETCH works.

### `hibernate.dialect=MySQL8Dialect`
Tells Hibernate which SQL dialect to use when generating queries. MySQL 8 dialect enables:
- `LIMIT`/`OFFSET` syntax for pagination
- MySQL-specific functions (`NOW()`, `DATE()`)
- Proper handling of `TEXT`, `BIGINT`, `DATETIME` types

### `format_sql=true`
Pretty-prints the SQL (with indentation) when `show-sql=true`. Without this, all SQL is on one line.

---

## Thymeleaf

```properties
spring.thymeleaf.cache=false
spring.thymeleaf.prefix=classpath:/templates/
spring.thymeleaf.suffix=.html
```

### `cache=false`
Thymeleaf templates are re-read from disk on every request — changes to `.html` files are visible immediately without restarting the app. **Set to `true` in production** for performance.

### `prefix` and `suffix`
When a controller returns `"admin/dashboard"`, Thymeleaf constructs:
```
classpath:/templates/ + admin/dashboard + .html
= src/main/resources/templates/admin/dashboard.html
```

These are the default values and could be omitted, but they are explicit here for clarity.

---

## Application Name

```properties
spring.application.name=online-shopping-system
```

Used in Spring Boot Actuator metrics, logging, and distributed tracing. Not functionally required for this project but is a best-practice identifier.

---

## Commented-Out: data.sql Loading

```properties
# spring.sql.init.mode=always
# spring.jpa.defer-datasource-initialization=true
```

These two properties were needed when `data.sql` was the seeding mechanism. Now `DataInitializer.java` handles seeding via `CommandLineRunner`, so these are disabled.

**Why `defer-datasource-initialization=true` was needed:** By default, `data.sql` runs before Hibernate creates tables. `defer-datasource-initialization=true` makes it run after Hibernate finishes, so the tables exist when INSERT statements run.

---

## Email (Not in this file — in .env)

The email SMTP properties (`spring.mail.*`) are not in `application.properties` — they are in `.env`:

```
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=465
SPRING_MAIL_USERNAME=...
SPRING_MAIL_PASSWORD=...  (Gmail App Password)
SPRING_MAIL_PROPERTIES_MAIL_SMTP_SSL_ENABLE=true
```

Spring Boot's `@ConfigurationProperties` reads environment variables with prefix `SPRING_MAIL_` and maps them to `spring.mail.*` properties automatically (dots become underscores in env var names).

---

## Full Property Loading Order (Spring Boot)

1. Default values hardcoded in Spring Boot starters
2. `application.properties` (this file)
3. `application-{profile}.properties` (if a profile is active)
4. Environment variables (OS or `.env` sourced by shell)
5. Command-line arguments (`--server.port=9090`)

Later sources override earlier ones.

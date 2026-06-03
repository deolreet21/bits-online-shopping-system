# pom.xml

**File:** `pom.xml`  
**Owner:** Mehwish  
**Type:** Maven Project Object Model  
**Purpose:** Declares the project's Java version, Spring Boot parent, all library dependencies, and the build plugin. Maven reads this file to download JARs, compile the project, and run it.

---

## Project Coordinates

```xml
<groupId>com.shopping</groupId>
<artifactId>online-shopping-system</artifactId>
<version>1.0.0</version>
```

- **groupId** — organization identifier (reverse domain style)
- **artifactId** — the project name, used as the JAR file name
- **version** — `1.0.0` = first stable release

---

## Parent

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.0</version>
    <relativePath/>
</parent>
```

**Why parent?** Spring Boot's parent POM provides:
1. Pre-configured dependency versions — no version tag needed on most Spring dependencies
2. Maven plugin defaults (compiler plugin set to Java 17)
3. Resource filtering for `application.properties`

`<relativePath/>` — tells Maven to look in Maven Central (not the local filesystem) for this parent.

---

## Java Version

```xml
<properties>
    <java.version>17</java.version>
</properties>
```

Sets source and target bytecode to Java 17. Required for records, text blocks, and pattern matching used in the project.

---

## Dependencies Explained

### `spring-boot-starter-web`
Bundles: Spring MVC + embedded Tomcat + Jackson JSON.  
**Why:** Powers all `@Controller` endpoints, HTTP request/response handling, and the built-in web server. No separate Tomcat install needed — the app is a self-contained JAR.

### `spring-boot-starter-thymeleaf`
Adds Thymeleaf template engine + Spring MVC integration.  
**Why:** Server-side rendering of `.html` templates in `src/main/resources/templates/`. The `th:` namespace attributes are processed here.

### `spring-boot-starter-data-jpa`
Bundles: Spring Data JPA + Hibernate ORM + transaction management.  
**Why:** All `JpaRepository` interfaces, `@Entity` mapping, JPQL queries, and `@Transactional` are powered by this.

### `spring-boot-starter-validation`
Adds Jakarta Bean Validation (Hibernate Validator implementation).  
**Why:** Enables `@NotNull`, `@Min`, `@Max`, `@Email` on entity fields (even though most validation in this project is manual in controllers, this dependency is required for the validation infrastructure).

### `mysql-connector-j` (scope: runtime)
The JDBC driver for MySQL.  
**Why runtime scope:** Not needed at compile time (the code uses JDBC interfaces, not MySQL-specific classes). Only needed when the app actually runs and connects to MySQL.

```xml
<scope>runtime</scope>
```

### `spring-boot-devtools` (scope: runtime, optional: true)
Enables hot reload during development — restarts the app when classes change.  
**Why optional:** Does not get included in the final production JAR when another project depends on this one. `runtime` scope = not on compile classpath.

### `spring-security-crypto`
Provides `BCryptPasswordEncoder` only — NOT full Spring Security.  
**Why this instead of `spring-boot-starter-security`?** Full Spring Security would auto-configure form login, CSRF protection, and HTTP Basic auth, which would conflict with the custom session-based authentication built in `AuthController` and `SessionInterceptor`. This project uses Spring Security only for BCrypt password hashing.

### `spring-boot-starter-mail`
Bundles JavaMail + Spring's `JavaMailSender` abstraction.  
**Why:** Powers `EmailNotificationService` — Gmail SMTP sending on port 465. Without this dependency, `@Autowired JavaMailSender` would not resolve.

### `spring-boot-starter-test` (scope: test)
Bundles JUnit 5 + Mockito + AssertJ + Spring Test.  
**Why test scope:** Only on the test classpath. Not included in production JAR.

---

## Build Plugin

```xml
<plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
</plugin>
```

**What it does:**
- `mvn spring-boot:run` — runs the app in-process with hot reload
- `mvn package` — creates a fat/uber JAR with all dependencies embedded (e.g., `online-shopping-system-1.0.0.jar`)

**Fat JAR:** All dependency JARs are nested inside the output JAR. The result is one file that runs anywhere with `java -jar`.

---

## Dependency Resolution Flow

```
pom.xml (parent: spring-boot-starter-parent 3.2.0)
  │
  ├── spring-boot-starter-web
  │     └── spring-webmvc + embedded-tomcat + jackson-databind
  │
  ├── spring-boot-starter-thymeleaf
  │     └── thymeleaf-spring6 + thymeleaf-extras-java8time
  │
  ├── spring-boot-starter-data-jpa
  │     └── hibernate-core + spring-data-jpa + spring-orm
  │
  ├── spring-security-crypto
  │     └── BCryptPasswordEncoder (only)
  │
  └── spring-boot-starter-mail
        └── jakarta.mail + spring-context-support
```

---

## What Happens at `mvn spring-boot:run`

1. Maven reads `pom.xml`, resolves all dependencies from `~/.m2/repository`
2. Compiles all `.java` files → `.class` files
3. Starts embedded Tomcat on port 8080
4. Spring's component scan finds all `@SpringBootApplication` → discovers all beans
5. Hibernate reads all `@Entity` classes → creates/updates MySQL tables (`ddl-auto=update`)
6. `DataInitializer.run()` seeds the database if empty
7. App is live at `http://localhost:8080`

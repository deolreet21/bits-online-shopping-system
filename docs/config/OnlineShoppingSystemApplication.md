# OnlineShoppingSystemApplication.java

**File:** `src/main/java/com/shopping/system/OnlineShoppingSystemApplication.java`  
**Owner:** HeenuReet  
**Type:** Spring Boot entry point  
**Purpose:** The `main` class that boots the entire application. Also declares the `BCryptPasswordEncoder` bean so it can be injected anywhere in the app.

---

## Full Source

```java
@SpringBootApplication
public class OnlineShoppingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(OnlineShoppingSystemApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## `@SpringBootApplication`

This single annotation is a shorthand for three annotations:

| Composed Annotation | What It Does |
|---------------------|-------------|
| `@Configuration` | This class can declare `@Bean` methods |
| `@EnableAutoConfiguration` | Spring Boot auto-configures beans based on classpath (e.g., sees Hibernate on classpath → auto-configures DataSource) |
| `@ComponentScan` | Scans the package `com.shopping.system` and all sub-packages for `@Component`, `@Service`, `@Repository`, `@Controller` |

**Why one annotation?** Boot 2+ composites them as a convenience. Equivalent to writing all three separately.

---

## `SpringApplication.run(...)`

```java
SpringApplication.run(OnlineShoppingSystemApplication.class, args);
```

This single call:
1. Creates the Spring `ApplicationContext` (the IoC container)
2. Runs auto-configuration (sets up DataSource, EntityManager, Tomcat, etc.)
3. Component-scans for all annotated classes
4. Wires all `@Autowired` dependencies
5. Starts embedded Tomcat on port 8080
6. Runs all `CommandLineRunner` beans (i.e., `DataInitializer.run()`)
7. The app is live

`args` — passes command-line arguments (like `--server.port=9090`) to Spring.

---

## `@Bean` — BCryptPasswordEncoder

```java
@Bean
public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**`@Bean`** — declares this method's return value as a Spring-managed singleton bean. Spring calls this method once and stores the result in the application context.

**Why declare it here?** `BCryptPasswordEncoder` is not annotated with `@Component` (it's a library class, not ours to annotate). The only way to make a third-party class a Spring bean is via `@Bean` in a `@Configuration` class.

**Who uses it?** `UserService` injects it:
```java
@Autowired private BCryptPasswordEncoder passwordEncoder;
```

Spring sees the `BCryptPasswordEncoder` type, finds the `@Bean` method that returns it, and injects the singleton.

**Why a singleton?** `BCryptPasswordEncoder` is thread-safe and stateless. One instance shared across all service calls is correct and memory-efficient.

---

## Package Structure

This class is in `com.shopping.system` — the root package. `@ComponentScan` (included in `@SpringBootApplication`) defaults to scanning the package of the annotated class and all sub-packages:

```
com.shopping.system                           ← scanned
com.shopping.system.controller                ← scanned
com.shopping.system.service                   ← scanned
com.shopping.system.repository                ← scanned
com.shopping.system.entity                    ← scanned
```

If this class were moved to a different package, beans in `com.shopping.system` would not be found.

---

## Startup Sequence

```
main() called
  │
  └── SpringApplication.run(...)
        │
        ├── Create ApplicationContext
        ├── Auto-configure DataSource (MySQL)
        ├── Auto-configure EntityManagerFactory (Hibernate)
        ├── Auto-configure Tomcat on :8080
        ├── Component scan → register all @Service, @Repository, @Controller beans
        ├── Wire all @Autowired fields
        ├── Hibernate ddl-auto=update → sync DB schema
        ├── Run DataInitializer.run() (CommandLineRunner)
        └── App ready — first request can be handled
```

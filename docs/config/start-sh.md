# start.sh

**File:** `start.sh`  
**Owner:** HeenuReet  
**Type:** Bash shell script  
**Purpose:** One-command setup and launch for macOS. Handles the entire first-time environment setup: checks Java and Maven, installs Homebrew and MySQL if missing, starts MySQL, sets root password, creates the database, and finally launches the Spring Boot app.

---

## Why This Script Exists

Without `start.sh`, a new team member would need to:
1. Install Java 17
2. Install Maven
3. Install MySQL
4. Configure MySQL root password
5. Create the `shopping_system` database
6. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables
7. Run `mvn spring-boot:run`

`start.sh` automates steps 2–7 (assumes Java is pre-installed).

---

## Script Flow

```
start.sh
  │
  ├── Load .env (source .env → exports DB_URL, DB_USERNAME, DB_PASSWORD, etc.)
  │
  ├── 1. Java check — is java ≥ 17 available?
  │
  ├── 2. Maven check — mvn or ./mvnw?
  │
  ├── 3. Homebrew check — install if missing (needed for MySQL)
  │
  ├── 4. MySQL install — brew install mysql (if not present)
  │
  ├── 5. Start MySQL service — brew services start mysql
  │
  ├── 6. Wait for MySQL — mysqladmin ping loop (up to 30 seconds)
  │
  ├── 7. First-time password setup (only if .setup_done not present)
  │     ├── Case A: fresh install (no password) → ALTER USER to set Shopping@123
  │     ├── Case B: unknown password → init-file reset
  │     └── Case C: already configured → skip
  │
  ├── 8. Create database — CREATE DATABASE IF NOT EXISTS shopping_system
  │
  └── 9. Launch — $MVN spring-boot:run
```

---

## Key Variables

```bash
PROPS="src/main/resources/application.properties"
DB_NAME="shopping_system"
DB_USER="${DB_USERNAME:-root}"    # use DB_USERNAME env var or default to "root"
SETUP_DONE_FLAG=".setup_done"
```

**`${DB_USERNAME:-root}`** — Bash parameter expansion with default: if `DB_USERNAME` is unset/empty, use `"root"`.

**`.setup_done`** — A sentinel file created after first-time password setup. On subsequent runs, the entire password-setup block is skipped. This prevents re-running `ALTER USER` on every start.

---

## Section Annotations

### `.env` Loading

```bash
if [ -f .env ]; then
    source .env
fi
```

`source .env` runs the `.env` file in the current shell, exporting `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SPRING_MAIL_*`, etc. These become Spring's `${DB_URL}` placeholders.

### Java Version Check

```bash
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
```

- `java -version 2>&1` — version output goes to stderr; redirect to stdout
- `awk -F '"' '/version/ {print $2}'` — extract the quoted version string (e.g., `"17.0.2"`)
- `cut -d'.' -f1` — take only the major version (`17`)

### Maven Discovery

```bash
if command -v mvn &>/dev/null; then
    MVN=mvn
elif [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    MVN=./mvnw
```

Prefers globally-installed Maven. Falls back to the Maven Wrapper (`mvnw`) which downloads the correct Maven version automatically.

### MySQL Readiness Loop

```bash
RETRIES=15
until mysqladmin -u root ping --silent &>/dev/null; do
    sleep 2
    RETRIES=$((RETRIES - 1))
done
```

`until` = loop until the condition is true. `mysqladmin ping` returns 0 only when MySQL is accepting connections. Up to 30 seconds (15 × 2s).

### Password Setup — Case A (Fresh Install)

```bash
mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASS'; FLUSH PRIVILEGES;"
```

If MySQL was just installed, root has no password — connect without `-p` and set it.

**`FLUSH PRIVILEGES`** — tells MySQL to reload the grant tables from disk, applying the change immediately.

### Password Setup — Case B (Unknown Password Reset)

```bash
/opt/homebrew/bin/mysqld --init-file=/tmp/reset_pw.sql --user="$USER" &
```

Starts MySQL directly with an init-file that runs SQL before the auth system loads — bypassing the password check entirely. This is the standard MySQL root password recovery procedure.

### Database Creation

```bash
mysql -u "$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;"
```

`IF NOT EXISTS` — idempotent: safe to run on every startup. If the database already exists, this is a no-op.

### App Launch

```bash
$MVN spring-boot:run
```

This is the last line — the script blocks here until the app is stopped (Ctrl+C).

---

## Error Handling

Every critical step has:
```bash
if [ $? -ne 0 ]; then
    echo "ERROR: ..."
    exit 1
fi
```

`$?` is the exit code of the last command. `exit 1` terminates the script with failure, stopping the launch.

---

## Limitations

- **macOS only** — uses `brew services` which is macOS/Homebrew-specific
- **Assumes Homebrew path** — uses `/opt/homebrew` (Apple Silicon) but not `/usr/local` (Intel) in the init-file reset
- **Hardcodes `Shopping@123`** — the first-time password. On subsequent runs, `.env` credentials take over

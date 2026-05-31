#!/usr/bin/env bash
# start.sh — Automated e-Kiosk setup and launch (macOS)
# On first run: installs MySQL via Homebrew, creates the application database,
# and resets the root password. Subsequent runs start MySQL and launch the app.

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
SETUP_FLAG="$SCRIPT_DIR/.setup_done"
DB_NAME="shopping_system"
DB_PASS="Shopping@123"

echo "=== e-Kiosk — Automated Startup ==="

# ── 1. Install MySQL via Homebrew if not already present ─────────────────────
if ! brew list mysql &>/dev/null; then
    echo "MySQL not found — installing via Homebrew (this may take a few minutes)..."
    brew install mysql
fi

# ── 2. Ensure the MySQL service is running ────────────────────────────────────
brew services start mysql 2>/dev/null || true

# ── 3. Poll until MySQL accepts connections ───────────────────────────────────
# mysqladmin ping is more reliable than a fixed sleep — MySQL startup time
# varies, especially on first install when it must initialise its data directory.
echo -n "Waiting for MySQL"
until mysqladmin ping --silent 2>/dev/null; do
    echo -n "."
    sleep 1
done
echo " ready"

# ── 4. First-time setup — runs once, guarded by a .setup_done flag file ───────
if [ ! -f "$SETUP_FLAG" ]; then
    echo "Running first-time database setup..."

    # Fresh Homebrew MySQL starts with no root password — reset it and create DB.
    mysql -u root <<-SQL
        ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY '${DB_PASS}';
        FLUSH PRIVILEGES;
        CREATE DATABASE IF NOT EXISTS ${DB_NAME}
            CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SQL

    # Update the DB password in .env using macOS BSD sed.
    # Note: sed -i '' requires the empty string argument on macOS —
    # GNU/Linux sed uses sed -i without it.
    sed -i '' "s|DB_PASSWORD=.*|export DB_PASSWORD=\"${DB_PASS}\"|" "$ENV_FILE" 2>/dev/null || true

    touch "$SETUP_FLAG"
    echo "Database setup complete."
fi

# ── 5. Source environment variables and start the Spring Boot application ─────
echo "Launching e-Kiosk at http://localhost:8080 ..."
# shellcheck source=.env
source "$ENV_FILE"

JAVA_HOME=/Library/Java/JavaVirtualMachines/sapmachine-21.jdk/Contents/Home \
    "$SCRIPT_DIR/mvnw" -f "$SCRIPT_DIR/pom.xml" spring-boot:run

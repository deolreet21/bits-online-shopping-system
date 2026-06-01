#!/bin/bash
# Start script for Online Shopping System (e-Kiosk)
# Handles first-time setup automatically: installs MySQL, resets password, creates database

PROPS="src/main/resources/application.properties"
DB_NAME="shopping_system"
DB_USER="root"
SETUP_DONE_FLAG=".setup_done"

# ── Load .env if present ─────────────────────────
if [ -f .env ]; then
    source .env
    echo "[env] Loaded credentials from .env"
else
    echo "WARN: No .env file found. Copy .env.example to .env and fill in your values."
fi

echo "============================================"
echo "  Online Shopping System - e-Kiosk"
echo "============================================"

# ── 1. Java check ────────────────────────────────
if ! command -v java &>/dev/null; then
    echo "ERROR: Java not found."
    echo "  Install Java 17: https://adoptium.net"
    exit 1
fi
JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VER" -lt 17 ] 2>/dev/null; then
    echo "ERROR: Java 17+ required. Found version: $JAVA_VER"
    exit 1
fi
echo "Java OK: $(java -version 2>&1 | head -1)"

# ── 2. Maven check ───────────────────────────────
if command -v mvn &>/dev/null; then
    MVN=mvn
elif [ -f "./mvnw" ]; then
    chmod +x ./mvnw
    MVN=./mvnw
else
    echo "ERROR: Maven not found."
    echo "  Install Maven: brew install maven"
    exit 1
fi
echo "Maven OK: $MVN"

# ── 3. Homebrew check ────────────────────────────
echo ""
if ! command -v brew &>/dev/null; then
    echo "Homebrew not found. Installing..."
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    # Add brew to PATH for Apple Silicon
    eval "$(/opt/homebrew/bin/brew shellenv)" 2>/dev/null || eval "$(/usr/local/bin/brew shellenv)" 2>/dev/null
fi

# ── 4. MySQL install ─────────────────────────────
echo "Checking MySQL..."
if ! command -v mysql &>/dev/null; then
    echo "MySQL not found. Installing via Homebrew (this may take a few minutes)..."
    brew install mysql
    if [ $? -ne 0 ]; then
        echo "ERROR: Failed to install MySQL."
        exit 1
    fi
    echo "MySQL installed."
fi

# ── 5. Start MySQL service ───────────────────────
if ! brew services list | grep -E "^mysql\s" | grep -q "started"; then
    echo "Starting MySQL service..."
    brew services start mysql
fi

# Wait for MySQL to accept connections (up to 30 seconds)
echo "Waiting for MySQL to be ready..."
RETRIES=15
until mysqladmin -u root ping --silent &>/dev/null; do
    if [ $RETRIES -eq 0 ]; then
        echo "ERROR: MySQL did not start in time."
        echo "  Check status with: brew services list"
        exit 1
    fi
    sleep 2
    RETRIES=$((RETRIES - 1))
done
echo "MySQL is running."

# ── 6. First-time password setup ─────────────────
if [ ! -f "$SETUP_DONE_FLAG" ]; then
    echo ""
    echo "First-time setup: configuring MySQL root password..."

    NEW_PASS="Shopping@123"

    # Check if root has no password (fresh install)
    if mysql -u root -e "SELECT 1;" &>/dev/null; then
        echo "  Root has no password — setting password..."
        mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASS'; FLUSH PRIVILEGES;" 2>/dev/null || \
        mysql -u root -e "SET GLOBAL validate_password.policy=LOW; SET GLOBAL validate_password.length=4; ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASS'; FLUSH PRIVILEGES;" 2>/dev/null
        # Update application.properties
        sed -i '' "s|^spring.datasource.password=.*|spring.datasource.password=$NEW_PASS|" "$PROPS"
        echo "  Password set to: $NEW_PASS"

    # Root has a password already — use init-file reset
    elif ! mysql -u root -p"$(grep 'spring.datasource.password' $PROPS | cut -d'=' -f2 | tr -d ' ')" -e "SELECT 1;" &>/dev/null; then
        echo "  Root password unknown — resetting via init-file..."
        pkill -f mysqld 2>/dev/null; sleep 3

        cat > /tmp/_reset_pw.sql << EOF
ALTER USER 'root'@'localhost' IDENTIFIED BY '$NEW_PASS';
FLUSH PRIVILEGES;
EOF
        /opt/homebrew/bin/mysqld --init-file=/tmp/reset_pw.sql --user="$USER" &
        sleep 8

        # Verify reset worked
        if mysql -u root -p"$NEW_PASS" -e "SELECT 1;" &>/dev/null; then
            sed -i '' "s|^spring.datasource.password=.*|spring.datasource.password=$NEW_PASS|" "$PROPS"
            echo "  Password reset to: $NEW_PASS"
            # Restart cleanly via brew
            pkill -f mysqld 2>/dev/null; sleep 2
            brew services start mysql; sleep 5
        else
            echo "ERROR: Could not reset MySQL password automatically."
            echo "  Run manually: mysql_secure_installation"
            exit 1
        fi
    else
        echo "  MySQL credentials already configured."
    fi

    touch "$SETUP_DONE_FLAG"
fi

# ── 7. Read final credentials ────────────────────
DB_PASS=$(grep 'spring.datasource.password' "$PROPS" | cut -d'=' -f2 | tr -d ' ')

# ── 8. Create database ───────────────────────────
if mysql -u "$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME;" 2>/dev/null; then
    echo "Database '$DB_NAME' ready."
else
    echo "ERROR: Could not create database '$DB_NAME'."
    echo "  Check credentials in: $PROPS"
    exit 1
fi

# ── 9. Launch app ────────────────────────────────
echo ""
echo "============================================"
echo "  Starting Spring Boot application..."
echo "  URL:  http://localhost:8080"
echo ""
echo "  Default logins (password: password123)"
echo "    Admin    -> admin"
echo "    Customer -> heenureet / aliya / mehwish"
echo "============================================"
echo ""

$MVN spring-boot:run

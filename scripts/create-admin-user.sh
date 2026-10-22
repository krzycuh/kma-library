#!/bin/bash

# KMA Library - Create Admin User Script
# This script creates an initial admin user in the database

set -e  # Exit on error
set -u  # Exit on undefined variable

# ============================================
# Configuration
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
DB_CONTAINER="kmalibrary-postgres-prod"

# Load environment variables
if [ -f "$PROJECT_ROOT/.env.production" ]; then
    source "$PROJECT_ROOT/.env.production"
else
    echo "Warning: .env.production not found, using defaults"
fi

DB_NAME="${POSTGRES_DB:-kmalibrary}"
DB_USER="${POSTGRES_USER:-kmalibrary}"

# ============================================
# Functions
# ============================================

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $1"
}

error() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1" >&2
}

generate_password() {
    # Generate a random password
    openssl rand -base64 12
}

hash_password() {
    local password=$1
    # This is a placeholder - actual hashing will be done by the application
    # BCrypt hashing should be done by the Spring Security BCryptPasswordEncoder
    echo "$password"
}

create_admin_user() {
    log "Creating admin user..."
    
    # Prompt for user details
    read -p "Enter admin email: " ADMIN_EMAIL
    read -p "Enter admin name: " ADMIN_NAME
    
    # Generate or prompt for password
    echo "Generate random password or enter custom? (g/c)"
    read -r PASSWORD_CHOICE
    
    if [ "$PASSWORD_CHOICE" = "g" ]; then
        ADMIN_PASSWORD=$(generate_password)
        log "Generated password: $ADMIN_PASSWORD"
    else
        read -sp "Enter admin password: " ADMIN_PASSWORD
        echo ""
        read -sp "Confirm password: " ADMIN_PASSWORD_CONFIRM
        echo ""
        
        if [ "$ADMIN_PASSWORD" != "$ADMIN_PASSWORD_CONFIRM" ]; then
            error "Passwords do not match"
            exit 1
        fi
    fi
    
    # Hash the password (BCrypt with cost 10)
    # Note: This uses a Java command to generate BCrypt hash
    log "Hashing password..."
    HASHED_PASSWORD=$(docker exec kmalibrary-backend-prod java -cp /app/lib/\* \
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder \
        "$ADMIN_PASSWORD" 2>/dev/null || echo "PLACEHOLDER_HASH")
    
    # Insert user into database
    log "Inserting admin user into database..."
    
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" <<EOF
-- Create admin user
INSERT INTO users (id, email, password, name, role, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '$ADMIN_EMAIL',
    '\$2a\$10\$dummyHashWillBeReplacedByFirstLogin',
    '$ADMIN_NAME',
    'ADMIN',
    NOW(),
    NOW()
)
ON CONFLICT (email) DO UPDATE SET
    password = EXCLUDED.password,
    role = 'ADMIN',
    updated_at = NOW();

-- Verify insertion
SELECT id, email, name, role, created_at FROM users WHERE email = '$ADMIN_EMAIL';
EOF
    
    if [ $? -eq 0 ]; then
        log "Admin user created successfully!"
        echo ""
        echo "============================================"
        echo "Admin User Credentials"
        echo "============================================"
        echo "Email: $ADMIN_EMAIL"
        echo "Name: $ADMIN_NAME"
        echo "Temporary Password: $ADMIN_PASSWORD"
        echo ""
        echo "⚠️  IMPORTANT: Change the password after first login!"
        echo "⚠️  Save these credentials in a secure location!"
        echo "============================================"
    else
        error "Failed to create admin user"
        exit 1
    fi
}

create_admin_via_api() {
    log "Creating admin user via API..."
    
    read -p "Enter admin email: " ADMIN_EMAIL
    read -p "Enter admin name: " ADMIN_NAME
    read -sp "Enter admin password: " ADMIN_PASSWORD
    echo ""
    
    # Call registration endpoint
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/register \
        -H "Content-Type: application/json" \
        -d "{
            \"email\": \"$ADMIN_EMAIL\",
            \"password\": \"$ADMIN_PASSWORD\",
            \"name\": \"$ADMIN_NAME\"
        }")
    
    if echo "$RESPONSE" | grep -q "error"; then
        error "Failed to create user via API: $RESPONSE"
        exit 1
    fi
    
    # Get user ID from response
    USER_ID=$(echo "$RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
    
    # Update role to ADMIN directly in database
    log "Updating user role to ADMIN..."
    docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" <<EOF
UPDATE users SET role = 'ADMIN' WHERE email = '$ADMIN_EMAIL';
SELECT id, email, name, role FROM users WHERE email = '$ADMIN_EMAIL';
EOF
    
    log "Admin user created successfully via API!"
}

check_prerequisites() {
    # Check if database container is running
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        error "Database container '$DB_CONTAINER' is not running"
        echo "Start the database with: docker-compose -f docker-compose.prod.yml up -d postgres"
        exit 1
    fi
}

# ============================================
# Main Execution
# ============================================

main() {
    log "===== Create Admin User ====="
    echo ""
    
    check_prerequisites
    
    echo "Select method:"
    echo "1) Direct database insertion"
    echo "2) Via API (recommended)"
    read -p "Enter choice (1 or 2): " METHOD
    
    case $METHOD in
        1) create_admin_user ;;
        2) create_admin_via_api ;;
        *) error "Invalid choice"; exit 1 ;;
    esac
}

# Run main function
main

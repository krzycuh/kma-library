#!/bin/bash

# KMA Library - Database Restore Script
# This script restores PostgreSQL database from backup

set -e  # Exit on error
set -u  # Exit on undefined variable

# ============================================
# Configuration
# ============================================

# Load environment variables
if [ -f .env.production ]; then
    source .env.production
else
    echo "Error: .env.production file not found"
    exit 1
fi

# Backup configuration
BACKUP_DIR="${BACKUP_PATH:-/mnt/kmalibrary-data/backups}/postgres"

# Database configuration
DB_CONTAINER="kmalibrary-postgres-prod"
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

list_backups() {
    log "Available backups:"
    echo ""
    
    local backups=($(find "$BACKUP_DIR" -name "kmalibrary_*.sql.gz" -type f | sort -r))
    
    if [ ${#backups[@]} -eq 0 ]; then
        echo "No backups found in $BACKUP_DIR"
        exit 1
    fi
    
    local i=1
    for backup in "${backups[@]}"; do
        local filename=$(basename "$backup")
        local size=$(du -h "$backup" | cut -f1)
        local date=$(stat -c %y "$backup" | cut -d' ' -f1-2)
        printf "%2d) %-40s  Size: %-8s  Date: %s\n" $i "$filename" "$size" "$date"
        i=$((i+1))
    done
    
    echo ""
}

select_backup() {
    local backups=($(find "$BACKUP_DIR" -name "kmalibrary_*.sql.gz" -type f | sort -r))
    
    echo "Select backup to restore (1-${#backups[@]}) or 'q' to quit:"
    read -r selection
    
    if [ "$selection" = "q" ]; then
        log "Restore cancelled by user"
        exit 0
    fi
    
    if ! [[ "$selection" =~ ^[0-9]+$ ]] || [ "$selection" -lt 1 ] || [ "$selection" -gt ${#backups[@]} ]; then
        error "Invalid selection"
        exit 1
    fi
    
    BACKUP_FILE="${backups[$((selection-1))]}"
    log "Selected backup: $(basename $BACKUP_FILE)"
}

confirm_restore() {
    echo ""
    echo "⚠️  WARNING: This will REPLACE the current database with the backup!"
    echo "Database: $DB_NAME"
    echo "Backup: $(basename $BACKUP_FILE)"
    echo ""
    echo "Are you sure you want to continue? (yes/no)"
    read -r confirmation
    
    if [ "$confirmation" != "yes" ]; then
        log "Restore cancelled by user"
        exit 0
    fi
}

create_pre_restore_backup() {
    log "Creating pre-restore backup of current database..."
    
    local pre_restore_backup="${BACKUP_DIR}/pre_restore_$(date +"%Y%m%d_%H%M%S").sql.gz"
    
    docker exec "$DB_CONTAINER" pg_dump \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --clean \
        --if-exists \
        --create \
        | gzip > "$pre_restore_backup"
    
    log "Pre-restore backup created: $(basename $pre_restore_backup)"
}

restore_backup() {
    log "Starting database restore..."
    
    # Stop the backend to prevent connections
    log "Stopping backend service..."
    docker-compose -f docker-compose.prod.yml stop backend
    
    # Restore database
    log "Restoring from backup..."
    gunzip -c "$BACKUP_FILE" | docker exec -i "$DB_CONTAINER" psql -U "$DB_USER" -d postgres
    
    if [ $? -eq 0 ]; then
        log "Database restore completed successfully"
    else
        error "Database restore failed"
        exit 1
    fi
    
    # Restart backend
    log "Starting backend service..."
    docker-compose -f docker-compose.prod.yml start backend
    
    log "Waiting for backend to be healthy..."
    sleep 10
}

verify_restore() {
    log "Verifying database restore..."
    
    # Check if database exists and is accessible
    docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "SELECT COUNT(*) FROM users;" &>/dev/null
    
    if [ $? -eq 0 ]; then
        log "Database verification successful"
    else
        error "Database verification failed"
        exit 1
    fi
}

# ============================================
# Main Execution
# ============================================

main() {
    log "===== KMA Library Database Restore ====="
    echo ""
    
    # Check if running
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        error "Database container '$DB_CONTAINER' is not running"
        exit 1
    fi
    
    # List and select backup
    list_backups
    select_backup
    
    # Confirm restore
    confirm_restore
    
    # Create safety backup
    create_pre_restore_backup
    
    # Perform restore
    restore_backup
    verify_restore
    
    log "===== Database Restore Completed Successfully ====="
}

# Run main function
main

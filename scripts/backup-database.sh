#!/bin/bash

# KMA Library - Database Backup Script
# This script creates PostgreSQL backups with rotation policy

set -e  # Exit on error
set -u  # Exit on undefined variable
set -o pipefail  # A failing pg_dump/gunzip fails the whole pipeline

# ============================================
# Configuration
# ============================================

# Work from the project root, so the script also runs from cron or another directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_ROOT"

# Load environment variables
if [ -f .env.production ]; then
    source .env.production
else
    echo "Error: .env.production file not found in $PROJECT_ROOT"
    exit 1
fi

# Backup configuration
BACKUP_DIR="${BACKUP_PATH:-/mnt/kmalibrary-data/backups}/postgres"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-30}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/kmalibrary_${TIMESTAMP}.sql.gz"

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

check_dependencies() {
    if ! command -v docker &> /dev/null; then
        error "Docker is not installed"
        exit 1
    fi
}

check_container() {
    if ! docker ps | grep -q "$DB_CONTAINER"; then
        error "Database container '$DB_CONTAINER' is not running"
        exit 1
    fi
}

create_backup_dir() {
    if [ ! -d "$BACKUP_DIR" ]; then
        log "Creating backup directory: $BACKUP_DIR"
        mkdir -p "$BACKUP_DIR"
    fi
}

create_backup() {
    log "Starting database backup..."
    log "Database: $DB_NAME"
    log "Backup file: $BACKUP_FILE"
    
    # Create backup using pg_dump (plain SQL, compressed once by gzip on the host)
    docker exec "$DB_CONTAINER" pg_dump \
        -U "$DB_USER" \
        -d "$DB_NAME" \
        --clean \
        --if-exists \
        --create \
        | gzip -9 > "$BACKUP_FILE"
    
    if [ $? -eq 0 ]; then
        BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
        log "Backup completed successfully"
        log "Backup size: $BACKUP_SIZE"
    else
        error "Backup failed"
        rm -f "$BACKUP_FILE"
        exit 1
    fi
}

verify_backup() {
    log "Verifying backup integrity..."
    
    # The archive must be valid gzip and hold a complete plain-SQL dump
    if gzip -t "$BACKUP_FILE" 2>/dev/null \
        && gunzip -c "$BACKUP_FILE" | tail -n 5 | grep -q "PostgreSQL database dump complete"; then
        log "Backup verification successful"
    else
        error "Backup verification failed - file may be corrupted"
        exit 1
    fi
}

rotate_backups() {
    log "Rotating old backups (keeping last $RETENTION_DAYS days)..."
    
    # Find and delete backups older than retention period
    find "$BACKUP_DIR" -name "kmalibrary_*.sql.gz" -type f -mtime +$RETENTION_DAYS -delete
    
    REMAINING_BACKUPS=$(find "$BACKUP_DIR" -name "kmalibrary_*.sql.gz" -type f | wc -l)
    log "Remaining backups: $REMAINING_BACKUPS"
}

send_notification() {
    local status=$1
    local message=$2
    
    # Log to system log
    logger -t kmalibrary-backup "$message"
    
    # TODO: Send email notification if configured
    # if [ -n "${SMTP_HOST:-}" ]; then
    #     echo "$message" | mail -s "KMA Library Backup: $status" admin@kmalibrary.local
    # fi
}

# ============================================
# Main Execution
# ============================================

main() {
    log "===== KMA Library Database Backup Started ====="
    
    # Pre-flight checks
    check_dependencies
    check_container
    create_backup_dir
    
    # Create backup
    if create_backup && verify_backup; then
        rotate_backups
        send_notification "SUCCESS" "Database backup completed successfully: $BACKUP_FILE"
        log "===== Backup Process Completed Successfully ====="
        exit 0
    else
        send_notification "FAILED" "Database backup failed"
        error "===== Backup Process Failed ====="
        exit 1
    fi
}

# Run main function
main

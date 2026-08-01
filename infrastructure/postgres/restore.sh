#!/usr/bin/env bash

set -euo pipefail

readonly postgres_container="${POSTGRES_CONTAINER:-hotel-booking-postgres}"
readonly postgres_username="${POSTGRES_USER:-hotel_booking}"

readonly databases=(
  hotel_service
  booking_service
  identity_service
  notification_service
)

if [[ "$#" -ne 2 || "$1" != "--replace" ]]; then
  echo "Usage: $0 --replace <backup-directory>" >&2
  echo "This replaces the current data in all service databases." >&2
  exit 1
fi

readonly backup_directory="$2"

if [[ ! -f "$backup_directory/SHA256SUMS" ]]; then
  echo "Backup checksum manifest was not found: $backup_directory/SHA256SUMS" >&2
  exit 1
fi

if ! docker inspect "$postgres_container" >/dev/null 2>&1; then
  echo "PostgreSQL container '$postgres_container' is not running." >&2
  exit 1
fi

(
  cd "$backup_directory"
  sha256sum --check SHA256SUMS
)

for database in "${databases[@]}"; do
  backup_file="$backup_directory/$database.dump"

  if [[ ! -f "$backup_file" ]]; then
    echo "Database backup was not found: $backup_file" >&2
    exit 1
  fi

  echo "Restoring $database..."
  docker exec --interactive "$postgres_container" \
    pg_restore \
    --clean \
    --if-exists \
    --no-owner \
    --no-privileges \
    --username="$postgres_username" \
    --dbname="$database" < "$backup_file"
done

echo "Restore completed. Restart the application services before using the system."

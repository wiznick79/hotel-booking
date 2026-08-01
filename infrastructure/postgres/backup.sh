#!/usr/bin/env bash

set -euo pipefail

readonly project_directory="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
readonly backup_root_directory="${BACKUP_DIRECTORY:-$project_directory/infrastructure/.data/backups}"
readonly postgres_container="${POSTGRES_CONTAINER:-hotel-booking-postgres}"
readonly postgres_username="${POSTGRES_USER:-hotel_booking}"
readonly timestamp="${BACKUP_TIMESTAMP:-$(date -u +%Y%m%dT%H%M%SZ)}"
readonly backup_directory="$backup_root_directory/$timestamp"

readonly databases=(
  hotel_service
  booking_service
  identity_service
  notification_service
)

if ! docker inspect "$postgres_container" >/dev/null 2>&1; then
  echo "PostgreSQL container '$postgres_container' is not running." >&2
  exit 1
fi

mkdir -p "$backup_directory"
chmod 700 "$backup_directory"

for database in "${databases[@]}"; do
  backup_file="$backup_directory/$database.dump"

  echo "Backing up $database..."
  docker exec "$postgres_container" \
    pg_dump \
    --format=custom \
    --no-owner \
    --no-privileges \
    --username="$postgres_username" \
    "$database" > "$backup_file"
done

cat > "$backup_directory/metadata.txt" <<EOF
created-at-utc=$timestamp
postgres-container=$postgres_container
databases=${databases[*]}
EOF

(
  cd "$backup_directory"
  sha256sum ./*.dump > SHA256SUMS
)

echo "Backup completed: $backup_directory"

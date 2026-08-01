#!/usr/bin/env bash

set -euo pipefail

readonly application_directory="/opt/hotel-booking/current"
readonly temporary_backup_directory="$(mktemp -d /var/tmp/hotel-booking-postgresql-backup.XXXXXX)"
readonly timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
readonly archive_file="/var/tmp/hotel-booking-postgresql-$timestamp.tar.gz"
readonly aws_region="eu-west-3"
readonly backup_bucket_parameter="/hotel-booking/staging/backup-bucket-name"

cleanup() {
  rm -rf "$temporary_backup_directory"
  rm -f "$archive_file"
}

trap cleanup EXIT
umask 077

backup_bucket="$(aws ssm get-parameter \
  --region "$aws_region" \
  --name "$backup_bucket_parameter" \
  --query 'Parameter.Value' \
  --output text)"

BACKUP_DIRECTORY="$temporary_backup_directory" \
  "$application_directory/infrastructure/postgres/backup.sh"

tar --directory="$temporary_backup_directory" --create --gzip --file="$archive_file" "$timestamp"

aws s3 cp \
  "$archive_file" \
  "s3://$backup_bucket/backups/$timestamp.tar.gz" \
  --region "$aws_region" \
  --sse AES256

echo "PostgreSQL backup uploaded: s3://$backup_bucket/backups/$timestamp.tar.gz"

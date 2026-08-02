#!/usr/bin/env bash

set -euo pipefail

readonly application_directory="/opt/hotel-booking/current"
readonly runtime_directory="/opt/hotel-booking/runtime"
readonly parameter_prefix="/hotel-booking/staging"
readonly aws_region="eu-west-3"

if [[ ! -f "$application_directory/docker-compose.yml" ]]; then
  echo "Expected deployment files were not found in $application_directory." >&2
  exit 1
fi

read_parameter() {
  local parameter_name="$1"

  aws ssm get-parameter \
    --region "$aws_region" \
    --name "$parameter_prefix/$parameter_name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text
}

read_optional_parameter() {
  local parameter_name="$1"
  local default_value="$2"

  aws ssm get-parameter \
    --region "$aws_region" \
    --name "$parameter_prefix/$parameter_name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text 2>/dev/null || printf '%s\n' "$default_value"
}

install -d -m 700 "$runtime_directory"

temporary_environment_file=$(mktemp "$runtime_directory/.env.XXXXXX")
trap 'rm -f "$temporary_environment_file"' EXIT

umask 077

cat > "$temporary_environment_file" <<EOF
POSTGRES_USER=hotel_booking
POSTGRES_PASSWORD=$(read_parameter postgres-password)
JWT_PRIVATE_KEY_BASE64=$(read_parameter jwt-private-key-base64)
JWT_PUBLIC_KEY_BASE64=$(read_parameter jwt-public-key-base64)
GUEST_ACCESS_ENCRYPTION_SECRET=$(read_parameter guest-access-encryption-secret)
IDENTITY_BOOTSTRAP_USERNAME=$(read_parameter identity-bootstrap-username)
IDENTITY_BOOTSTRAP_PASSWORD=$(read_parameter identity-bootstrap-password)
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=$(read_parameter grafana-admin-password)
USE_SWAGGER=false
CORS_ALLOWED_ORIGINS=$(read_parameter cors-allowed-origins)
NOTIFICATION_EMAIL_FROM=$(read_parameter notification-email-from)
MAIL_HOST=$(read_optional_parameter notification-email-smtp-host mailpit)
MAIL_PORT=$(read_optional_parameter notification-email-smtp-port 1025)
MAIL_USERNAME=$(read_optional_parameter notification-email-smtp-username '')
MAIL_PASSWORD=$(read_optional_parameter notification-email-smtp-password '')
MAIL_SMTP_AUTH=$(read_optional_parameter notification-email-smtp-auth false)
MAIL_SMTP_STARTTLS_ENABLE=$(read_optional_parameter notification-email-smtp-starttls-enable false)
MAIL_SMTP_STARTTLS_REQUIRED=$(read_optional_parameter notification-email-smtp-starttls-required false)
EOF

mv "$temporary_environment_file" "$runtime_directory/.env"

compose() {
  docker compose \
    --env-file "$runtime_directory/.env" \
    -f "$application_directory/docker-compose.yml" \
    -f "$application_directory/docker-compose.staging.yml" \
    "$@"
}

compose build hotel-service
compose build booking-service
compose build identity-service
compose build notification-service
compose build api-gateway
compose build admin-web
compose build public-web

compose up --detach --remove-orphans

compose up --detach --force-recreate caddy

install -m 0644 \
  "$application_directory/infrastructure/aws/staging/hotel-booking-postgres-backup.service" \
  /etc/systemd/system/hotel-booking-postgres-backup.service
install -m 0644 \
  "$application_directory/infrastructure/aws/staging/hotel-booking-postgres-backup.timer" \
  /etc/systemd/system/hotel-booking-postgres-backup.timer

systemctl daemon-reload
systemctl enable --now hotel-booking-postgres-backup.timer

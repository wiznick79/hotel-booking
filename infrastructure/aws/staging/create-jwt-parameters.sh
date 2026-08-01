#!/usr/bin/env bash

set -euo pipefail

readonly parameter_prefix="/hotel-booking/staging"
readonly aws_region="${AWS_REGION:-eu-west-3}"

if ! command -v aws >/dev/null 2>&1; then
  echo "The AWS CLI must be installed and authenticated before running this script." >&2
  exit 1
fi

if ! command -v openssl >/dev/null 2>&1; then
  echo "OpenSSL must be installed before running this script." >&2
  exit 1
fi

temporary_directory=$(mktemp -d)
trap 'rm -rf "$temporary_directory"' EXIT

private_key_file="$temporary_directory/jwt-private.pem"

openssl genpkey \
  -algorithm RSA \
  -pkeyopt rsa_keygen_bits:2048 \
  -out "$private_key_file" \
  2>/dev/null

private_key_base64=$(openssl pkcs8 \
  -topk8 \
  -nocrypt \
  -in "$private_key_file" \
  -outform DER | openssl base64 -A)
public_key_base64=$(openssl pkey \
  -in "$private_key_file" \
  -pubout \
  -outform DER | openssl base64 -A)

MSYS_NO_PATHCONV=1 aws ssm put-parameter \
  --region "$aws_region" \
  --name "$parameter_prefix/jwt-private-key-base64" \
  --type SecureString \
  --value "$private_key_base64" \
  --overwrite >/dev/null

MSYS_NO_PATHCONV=1 aws ssm put-parameter \
  --region "$aws_region" \
  --name "$parameter_prefix/jwt-public-key-base64" \
  --type String \
  --value "$public_key_base64" \
  --overwrite >/dev/null

echo "Stored a new RSA signing key pair in AWS Systems Manager Parameter Store."
echo "Deploy the staging stack to make the new key pair active."

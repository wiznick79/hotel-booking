# AWS Terraform

This directory contains the Infrastructure as Code configuration for a small AWS staging environment.

## Current scope

The Terraform configuration currently creates:

- one dedicated VPC;
- one public subnet and its route to the internet;
- one security group that allows HTTP and HTTPS, but intentionally **not SSH**.
- one Amazon Linux 2023 EC2 host with an encrypted volume and IMDSv2 required;
- one Elastic IP address for stable staging DNS;
- one minimal EC2 role that permits AWS Systems Manager Session Manager access;
- a private, encrypted S3 backup prefix with a 14-day retention policy.

On its first boot, the host installs Docker, Git, and a checksum-verified Docker Compose plugin, enables the SSM agent, and creates `/opt/hotel-booking`. It deliberately does **not** clone or start the Compose stack: this repository is private, and a GitHub token must never be embedded in EC2 user data or Terraform state. A later CI/CD step will authenticate to AWS with GitHub OpenID Connect and deliver a built application artifact.

The host uses AWS Systems Manager Session Manager for administration instead of exposing port 22. Its Elastic IP makes the public endpoint stable for DNS, while only HTTP and HTTPS are permitted through the security group. Terraform ignores later `user_data` changes for this learning host because cloud-init executes only on its first boot; a production design would use an immutable image or launch-template rollout instead.

## PostgreSQL backups

The staging deployment configures a systemd timer that backs up all four PostgreSQL
service databases each day at 02:30 UTC. The host can write only to the `backups/`
prefix of the existing private deployment bucket. Backup archives use S3 server-side
encryption and expire after 14 days. See `docs/operations/postgresql-backups.md` for
manual backup, restore, and teardown instructions.

## Prerequisites

1. Install Terraform.
2. Authenticate the AWS CLI using IAM Identity Center or another short-lived credential mechanism:

   ```powershell
   aws login
   aws sts get-caller-identity
   ```

3. Copy `terraform.tfvars.example` to `terraform.tfvars` only if you need to override defaults. It is ignored by Git.

The default `t3.large` instance has 8 GiB of RAM because the current Docker Compose stack includes five Spring services, PostgreSQL, Kafka, Redis, Prometheus, Grafana, and Mailpit. It is a staging-learning choice, not a free-tier assumption; check its current cost before applying.

## Workflow

Run commands from this directory:

```powershell
terraform init
terraform fmt -recursive
terraform validate
terraform plan
```

`terraform plan` is read-only. It shows the exact AWS resources that would be added, changed, or destroyed.

Only run the following after reviewing the plan and confirming expected AWS costs:

```powershell
terraform apply
```

To remove every resource managed by this configuration:

```powershell
terraform destroy
```

## Access after apply

Use Session Manager rather than SSH:

```powershell
aws ssm start-session --target (terraform output -raw staging_host_instance_id)
```

If this command is denied, the IAM user needs permission to start an SSM session. The EC2 instance role is already granted the permissions it needs to register as a managed instance.

## State and secrets

Terraform writes `terraform.tfstate` locally by default. It contains resource identifiers and may contain sensitive values, so it is ignored by Git. Before a shared or long-lived environment, we will migrate the state to an encrypted remote backend.

Do not put passwords, JWT keys, database credentials, or SMTP credentials in Terraform variables or committed files. A later application layer will use AWS Secrets Manager or Parameter Store, with the real secret values supplied outside Git.

## GitHub Actions delivery

Terraform also defines a private S3 artifact bucket and a GitHub OpenID Connect deployment role. The role is restricted to this repository's immutable GitHub identity and the `main` branch. The manual `Deploy staging artifact` workflow packages the tested repository, uploads it to the private bucket, and uses Systems Manager to extract it on the EC2 host.

Before triggering the workflow, configure these GitHub repository variables from Terraform outputs:

- `AWS_DEPLOYMENT_BUCKET`;
- `AWS_DEPLOY_ROLE_ARN`;
- `AWS_STAGING_HOST_INSTANCE_ID`.

The workflow delivers source, then uses Systems Manager to start or update the staging Compose stack.

## Staging runtime configuration

The base `docker-compose.yml` remains a local-development setup. The host uses `docker-compose.staging.yml` to keep PostgreSQL, Kafka, Redis, Mailpit, and the individual services private. The gateway additionally binds to `127.0.0.1:8080` for host-side diagnostics; public requests pass through Caddy only.

The host-side `infrastructure/aws/staging/start-stack.sh` reads these encrypted Systems Manager Parameter Store values and writes a root-only runtime environment file outside the deployed Git checkout:

- `/hotel-booking/staging/postgres-password`;
- `/hotel-booking/staging/jwt-private-key-base64`;
- `/hotel-booking/staging/jwt-public-key-base64`;
- `/hotel-booking/staging/guest-access-encryption-secret`;
- `/hotel-booking/staging/identity-bootstrap-username`;
- `/hotel-booking/staging/identity-bootstrap-password`;
- `/hotel-booking/staging/grafana-admin-password`;
- `/hotel-booking/staging/cors-allowed-origins`;
- `/hotel-booking/staging/notification-email-from`.

Real parameter values are created outside Git. `infrastructure/aws/staging/.env.example` documents the resulting file shape but must never contain a real secret.

The staging identity service runs with the `postgres` profile only. On an empty identity database,
the two `identity-bootstrap-*` parameters create the first administrator without hotel assignments.
That administrator can then create the first hotel through the setup flow, which assigns it to their
account. Once the user exists, the password parameter is not used to overwrite it. Never enable the
`dev` profile in staging or production: it can seed the predictable development account `admin` /
`change-me`.

## Public HTTPS endpoint

The staging Compose override runs Caddy as the only public entry point. Caddy obtains and renews a certificate for `hotel.wiznick.net`, redirects HTTP to HTTPS, and proxies requests to the internal API gateway. PostgreSQL, Kafka, Redis, Mailpit, and the individual services have no public ports; the gateway is additionally bound to loopback for host-side diagnostics only.

The gateway trusts `X-Forwarded-For` only in the staging override, where Caddy is the public entry point. Caddy discards client-supplied forwarding headers before passing the real client address upstream, allowing public rate limits to operate per client rather than treating every request as Caddy.

The current learning host builds source on the EC2 instance. Its bootstrap script installs checksum-verified Docker Compose and Docker Buildx plugins, which Compose uses for service builds. To stay within the host's memory budget, the five Java images build sequentially. A later improvement will build immutable images in CI and pull them from a registry instead.

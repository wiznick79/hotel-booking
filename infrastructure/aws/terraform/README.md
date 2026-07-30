# AWS Terraform

This directory contains the Infrastructure as Code configuration for a small AWS staging environment.

## Current scope

The Terraform configuration currently creates:

- one dedicated VPC;
- one public subnet and its route to the internet;
- one security group that allows HTTP and HTTPS, but intentionally **not SSH**.
- one Amazon Linux 2023 EC2 host with an encrypted volume and IMDSv2 required;
- one minimal EC2 role that permits AWS Systems Manager Session Manager access.

On its first boot, the host installs Docker, Git, and a checksum-verified Docker Compose plugin, enables the SSM agent, and creates `/opt/hotel-booking`. It deliberately does **not** clone or start the Compose stack: this repository is private, and a GitHub token must never be embedded in EC2 user data or Terraform state. A later CI/CD step will authenticate to AWS with GitHub OpenID Connect and deliver a built application artifact.

The host uses AWS Systems Manager Session Manager for administration instead of exposing port 22. It has a public IP only so it can access package repositories and later receive HTTP/HTTPS traffic through a reverse proxy. Terraform ignores later `user_data` changes for this learning host because cloud-init executes only on its first boot; a production design would use an immutable image or launch-template rollout instead.

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

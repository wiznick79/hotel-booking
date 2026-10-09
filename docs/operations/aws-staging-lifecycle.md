# AWS staging lifecycle

## Purpose and current scope

The AWS environment is a temporary learning/staging deployment in `eu-west-3`
(Paris), not the production platform. It demonstrated the complete deployment
path while the daily development workflow remains local Docker Compose.

Terraform in `infrastructure/aws/terraform/` manages these resources:

- one VPC, public subnet, internet gateway, route table, and public web security group;
- one Amazon Linux 2023 EC2 Docker host with encrypted gp3 root volume and IMDSv2;
- one Elastic IP address used by `hotel.wiznick.net` and `admin.hotel.wiznick.net`;
- a host IAM role and instance profile with Systems Manager access;
- a private, encrypted, versioned S3 deployment-artifact bucket with 14-day backup retention;
- a GitHub Actions OIDC provider and a least-privilege deployment role;
- one SSM parameter containing the artifact-bucket name;
- an SES domain identity for `wiznick.net`, configured with Easy DKIM.

The staging host runs the Docker Compose staging override: Caddy, the two web
frontends, API gateway, four Spring services, PostgreSQL, Redis, Kafka, Mailpit,
Prometheus, and Grafana. Only Caddy is public; it terminates HTTPS and routes
`/api/*` to the gateway. SSH is intentionally not exposed. Administration uses
AWS Systems Manager Session Manager.

## What was learned

- Infrastructure as Code through Terraform: plan, apply, state, outputs, and destroy.
- Network isolation, security groups, encrypted EC2 storage, Elastic IPs, and IMDSv2.
- GitHub Actions OIDC: GitHub deploys without a stored long-lived AWS access key.
- Systems Manager Parameter Store for runtime configuration and Session Manager for host administration.
- Versioned S3 deployment artifacts and scheduled PostgreSQL backups.
- Caddy-managed HTTPS with public DNS records.
- SES sender-domain verification and DKIM.

As of 2026-08-03, Cost Explorer showed EC2 compute as the main cost driver. The
single `t3.large` is deliberately sized for the complete all-in-one Compose stack;
it is not economical to keep running for day-to-day development.

## Normal deployment

1. Apply Terraform and copy the output values to the GitHub repository variables:
   `AWS_DEPLOYMENT_BUCKET`, `AWS_DEPLOY_ROLE_ARN`, and
   `AWS_STAGING_HOST_INSTANCE_ID`.
2. Create the required SSM Parameter Store values under `/hotel-booking/staging/`.
   Keep the values in a password manager; they are deliberately not in Git.
3. Point `hotel.wiznick.net` and `admin.hotel.wiznick.net` A records at the
   `staging_host_elastic_ip` Terraform output.
4. Run the manual `Deploy staging artifact` GitHub workflow from `main`.
5. Caddy obtains certificates automatically. Confirm the public sites and
   `/actuator/health` through the gateway.

For the full parameter list, deployment process, and SMTP configuration, see the
Terraform [README](../../infrastructure/aws/terraform/README.md) and
[transactional email delivery](email-delivery.md).

## SES and DNS

`wiznick.net` was verified in SES with Easy DKIM. The three `_domainkey` CNAME
records live at Porkbun because DNS is not managed by Terraform. They permit
DKIM-signed messages from hotel-specific addresses such as
`morgadinha@wiznick.net`.

If Terraform destroys the SES identity, the old DKIM records are harmless but no
longer useful. A later apply creates a new SES identity and may generate different
DKIM records; retrieve `terraform output ses_dkim_dns_records` and update Porkbun.

SES SMTP credentials and production access were intentionally not configured. The
staging stack still uses Mailpit, so no guest email is delivered externally.

## Safe teardown

Terraform's reviewed destroy plan on 2026-08-03 contains 22 managed resources. It
destroys the EC2 host, Elastic IP, VPC/networking, S3 artifact and backup bucket,
IAM roles/OIDC provider, SES identity, and Terraform-managed SSM parameter. The
public websites immediately become unavailable.

Before teardown:

1. Create and verify a final PostgreSQL backup. Copy it outside AWS and verify its
   `SHA256SUMS`; see [PostgreSQL backups](postgresql-backups.md).
2. Remove or change the `hotel.wiznick.net` and `admin.hotel.wiznick.net` A records
   after the host is gone, because the Elastic IP will be released.
3. Ensure the local Terraform state is available. It is ignored by Git and identifies
   the existing managed resources.
4. While the full stack still exists, apply the explicit bucket-deletion setting.
   This records the Terraform provider setting before any resource is removed:

   ```powershell
   cd infrastructure/aws/terraform
   terraform apply -var='force_destroy_deployment_bucket=true'
   ```

   Review this apply carefully. It must not create or destroy infrastructure; it only
   updates Terraform's recorded bucket behavior.

5. Review the destroy plan:

   ```powershell
   terraform plan -destroy -var='force_destroy_deployment_bucket=true'
   ```

6. Only after confirming the final backup is safe, remove the stack:

   ```powershell
   terraform destroy -var='force_destroy_deployment_bucket=true'
   ```

`force_destroy_deployment_bucket` is intentionally false by default. The explicit
teardown flag permanently removes every versioned release artifact and backup in the
S3 bucket; without it, Terraform refuses to delete a non-empty bucket. Supplying the
flag only to `terraform destroy` is not enough because the provider reads the stored
bucket setting; apply it first while the complete state still exists.

Terraform does not manage the sensitive SSM parameters created separately. After
the infrastructure destroy, delete the `/hotel-booking/staging/` parameters from
Parameter Store, including PostgreSQL credentials, JWT keys, guest-access encryption
secret, bootstrap credentials, Grafana password, CORS origins, and any SMTP values.
This prevents unused secrets from remaining in AWS.

The GitHub repository variables will point to deleted resources after teardown. They
are harmless but stale; overwrite them from new Terraform outputs before the next
deployment.

## Recreate later

1. Authenticate with `aws login`, then run `terraform init`, `terraform validate`,
   `terraform plan`, and `terraform apply` in `infrastructure/aws/terraform/`.
2. Update the three GitHub deployment variables from Terraform outputs.
3. Recreate all required SSM parameter values from the password manager. Generate a
   new JWT key pair with `infrastructure/aws/staging/create-jwt-parameters.sh`.
4. Add the new Elastic IP to the two Porkbun A records.
5. Add or update the SES DKIM CNAME records from `ses_dkim_dns_records`, then wait
   for SES verification.
6. Trigger the manual deployment workflow from `main` and verify the public sites.

This preserves the learning environment as reproducible code while removing the
ongoing EC2 cost between cloud-testing sessions.

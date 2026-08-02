output "vpc_id" {
  description = "ID of the staging VPC."
  value       = aws_vpc.this.id
}

output "public_subnet_id" {
  description = "ID of the public subnet intended for the first EC2 deployment."
  value       = aws_subnet.public.id
}

output "public_web_security_group_id" {
  description = "Security group for the future public reverse-proxy endpoint."
  value       = aws_security_group.public_web.id
}

output "staging_host_instance_id" {
  description = "EC2 instance ID for Session Manager access."
  value       = aws_instance.staging_host.id
}

output "staging_host_elastic_ip" {
  description = "Stable Elastic IP address for the staging host. Point the staging DNS A record at this address."
  value       = aws_eip.staging_host.public_ip
}

output "deployment_artifact_bucket_name" {
  description = "Private S3 bucket used by GitHub Actions to deliver staging artifacts."
  value       = aws_s3_bucket.deployment_artifacts.id
}

output "github_deploy_role_arn" {
  description = "OIDC role assumed by the repository's main-branch GitHub Actions deployment workflow."
  value       = aws_iam_role.github_deploy.arn
}

output "ses_sending_domain" {
  description = "SES domain identity used for hotel guest emails."
  value       = aws_sesv2_email_identity.sending_domain.email_identity
}

output "ses_dkim_dns_records" {
  description = "Easy DKIM CNAME records to add at the DNS provider before sending email."
  value = {
    for token in aws_sesv2_email_identity.sending_domain.dkim_signing_attributes[0].tokens :
    "${token}._domainkey.${var.ses_sending_domain}" => "${token}.dkim.amazonses.com"
  }
}

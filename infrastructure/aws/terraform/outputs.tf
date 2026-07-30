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

output "staging_host_public_ip" {
  description = "Ephemeral public IP address of the staging host. It will change after stop/start until an Elastic IP is intentionally introduced."
  value       = aws_instance.staging_host.public_ip
}

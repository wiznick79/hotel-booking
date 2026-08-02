variable "aws_region" {
  description = "AWS region for this environment."
  type        = string
  default     = "eu-west-3"
}

variable "project_name" {
  description = "Short name used to identify resources."
  type        = string
  default     = "hotel-booking"
}

variable "environment" {
  description = "Deployment environment name."
  type        = string
  default     = "staging"
}

variable "vpc_cidr_block" {
  description = "IPv4 CIDR block allocated to the staging VPC."
  type        = string
  default     = "10.20.0.0/16"
}

variable "public_subnet_cidr_block" {
  description = "IPv4 CIDR block allocated to the public subnet."
  type        = string
  default     = "10.20.1.0/24"
}

variable "public_ingress_cidrs" {
  description = "CIDR blocks allowed to reach the public HTTP and HTTPS endpoints."
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "ec2_instance_type" {
  description = "Instance size for the all-in-one staging Docker host. t3.large provides 8 GiB of memory for the current local stack."
  type        = string
  default     = "t3.large"
}

variable "ec2_root_volume_size_gib" {
  description = "Size of the encrypted root volume for the staging Docker host."
  type        = number
  default     = 30
}

variable "ses_sending_domain" {
  description = "Domain verified with Amazon SES for hotel guest emails."
  type        = string
  default     = "wiznick.net"
}

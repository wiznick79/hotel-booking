resource "aws_security_group" "public_web" {
  name        = "${local.resource_name}-public-web"
  description = "Public HTTP and HTTPS access to the hotel-booking staging environment."
  vpc_id      = aws_vpc.this.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = var.public_ingress_cidrs
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = var.public_ingress_cidrs
  }

  egress {
    description = "Allow outbound access for package, image, and AWS API access."
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "${local.resource_name}-public-web"
  }
}

data "aws_ssm_parameter" "amazon_linux_2023_ami" {
  name = "/aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64"
}

resource "aws_instance" "staging_host" {
  ami                         = data.aws_ssm_parameter.amazon_linux_2023_ami.value
  instance_type               = var.ec2_instance_type
  subnet_id                   = aws_subnet.public.id
  vpc_security_group_ids      = [aws_security_group.public_web.id]
  iam_instance_profile        = aws_iam_instance_profile.staging_host.name
  associate_public_ip_address = true
  user_data_replace_on_change = false

  metadata_options {
    http_endpoint               = "enabled"
    http_tokens                 = "required"
    http_put_response_hop_limit = 1
  }

  root_block_device {
    encrypted   = true
    volume_size = var.ec2_root_volume_size_gib
    volume_type = "gp3"
  }

  user_data = <<-EOF
    #!/bin/bash
    set -euo pipefail

    dnf install -y docker git
    systemctl enable --now docker
    systemctl enable --now amazon-ssm-agent
    usermod -aG docker ec2-user

    install -d /usr/local/lib/docker/cli-plugins
    curl --fail --location --silent --show-error \
      --output /usr/local/lib/docker/cli-plugins/docker-compose \
      https://github.com/docker/compose/releases/download/v5.3.1/docker-compose-linux-x86_64
    echo "f9ebc6ebdb19d769b793c245a736caaeb198c62587f13b25c660c13b4987f959  /usr/local/lib/docker/cli-plugins/docker-compose" | sha256sum --check
    chmod 0755 /usr/local/lib/docker/cli-plugins/docker-compose

    install -d -o ec2-user -g ec2-user /opt/hotel-booking
  EOF

  lifecycle {
    ignore_changes = [user_data]
  }

  tags = {
    Name = "${local.resource_name}-host"
    Role = "docker-host"
  }
}

data "aws_iam_policy_document" "ec2_assume_role" {
  statement {
    effect = "Allow"

    principals {
      type        = "Service"
      identifiers = ["ec2.amazonaws.com"]
    }

    actions = ["sts:AssumeRole"]
  }
}

resource "aws_iam_role" "staging_host" {
  name               = "${local.resource_name}-host"
  assume_role_policy = data.aws_iam_policy_document.ec2_assume_role.json

  tags = {
    Name = "${local.resource_name}-host"
  }
}

resource "aws_iam_role_policy_attachment" "staging_host_ssm" {
  role       = aws_iam_role.staging_host.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

resource "aws_iam_instance_profile" "staging_host" {
  name = "${local.resource_name}-host"
  role = aws_iam_role.staging_host.name
}

data "aws_caller_identity" "current" {}

data "aws_partition" "current" {}

locals {
  artifact_bucket_name = "${local.resource_name}-${data.aws_caller_identity.current.account_id}-artifacts"
  github_oidc_subject  = "repo:wiznick79@49661706/hotel-booking@1313894872:ref:refs/heads/main"
}

resource "aws_s3_bucket" "deployment_artifacts" {
  bucket = local.artifact_bucket_name
}

resource "aws_s3_bucket_public_access_block" "deployment_artifacts" {
  bucket = aws_s3_bucket.deployment_artifacts.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "deployment_artifacts" {
  bucket = aws_s3_bucket.deployment_artifacts.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "deployment_artifacts" {
  bucket = aws_s3_bucket.deployment_artifacts.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "deployment_artifacts" {
  bucket = aws_s3_bucket.deployment_artifacts.id

  rule {
    id     = "expire-staging-postgresql-backups"
    status = "Enabled"

    filter {
      prefix = "backups/"
    }

    expiration {
      days = 14
    }

    noncurrent_version_expiration {
      noncurrent_days = 1
    }
  }
}

resource "aws_ssm_parameter" "staging_backup_bucket_name" {
  name  = "/${var.project_name}/${var.environment}/backup-bucket-name"
  type  = "String"
  value = aws_s3_bucket.deployment_artifacts.id
}

data "aws_iam_policy_document" "staging_host_artifact_read" {
  statement {
    effect = "Allow"

    actions = ["s3:ListBucket"]

    resources = [aws_s3_bucket.deployment_artifacts.arn]
  }

  statement {
    effect = "Allow"

    actions = ["s3:GetObject"]

    resources = ["${aws_s3_bucket.deployment_artifacts.arn}/releases/*"]
  }

  statement {
    effect = "Allow"

    actions = [
      "s3:AbortMultipartUpload",
      "s3:PutObject"
    ]

    resources = ["${aws_s3_bucket.deployment_artifacts.arn}/backups/*"]
  }

  statement {
    effect = "Allow"

    actions = ["ssm:GetParameter"]

    resources = [
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}:${data.aws_caller_identity.current.account_id}:parameter/hotel-booking/staging/*"
    ]
  }
}

resource "aws_iam_role_policy" "staging_host_artifact_read" {
  name   = "${local.resource_name}-artifact-read"
  role   = aws_iam_role.staging_host.id
  policy = data.aws_iam_policy_document.staging_host_artifact_read.json
}

resource "aws_iam_openid_connect_provider" "github_actions" {
  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_iam_policy_document" "github_deploy_assume_role" {
  statement {
    effect  = "Allow"
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [aws_iam_openid_connect_provider.github_actions.arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:sub"
      values   = [local.github_oidc_subject]
    }
  }
}

resource "aws_iam_role" "github_deploy" {
  name               = "${local.resource_name}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.github_deploy_assume_role.json
}

data "aws_iam_policy_document" "github_deploy" {
  statement {
    effect = "Allow"

    actions = [
      "s3:AbortMultipartUpload",
      "s3:GetObject",
      "s3:ListBucket",
      "s3:PutObject"
    ]

    resources = [
      aws_s3_bucket.deployment_artifacts.arn,
      "${aws_s3_bucket.deployment_artifacts.arn}/releases/*"
    ]
  }

  statement {
    effect = "Allow"

    actions = ["ssm:SendCommand"]

    resources = [
      "arn:${data.aws_partition.current.partition}:ec2:${var.aws_region}:${data.aws_caller_identity.current.account_id}:instance/${aws_instance.staging_host.id}",
      "arn:${data.aws_partition.current.partition}:ssm:${var.aws_region}::document/AWS-RunShellScript"
    ]
  }

  statement {
    effect = "Allow"

    actions   = ["ssm:GetCommandInvocation"]
    resources = ["*"]
  }
}

resource "aws_iam_role_policy" "github_deploy" {
  name   = "${local.resource_name}-deployment"
  role   = aws_iam_role.github_deploy.id
  policy = data.aws_iam_policy_document.github_deploy.json
}

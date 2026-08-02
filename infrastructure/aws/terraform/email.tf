resource "aws_sesv2_email_identity" "sending_domain" {
  email_identity = var.ses_sending_domain
}

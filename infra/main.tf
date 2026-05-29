locals {
  prefix = "${var.project_name}-${var.environment}"
}

data "aws_caller_identity" "current" {}

# ──────────────────────────────────────────────
# S3 Bucket (existing resource, kept as-is)
# ──────────────────────────────────────────────
resource "aws_s3_bucket" "bucket" {
  bucket = var.bucket_name
}

resource "aws_s3_bucket_public_access_block" "bucket" {
  bucket = aws_s3_bucket.bucket.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_versioning" "bucket" {
  bucket = aws_s3_bucket.bucket.id

  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "bucket" {
  bucket = aws_s3_bucket.bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "aws:kms"
    }
    bucket_key_enabled = true
  }
}

# ──────────────────────────────────────────────
# SNS Topic — receives feedback from Quarkus API
# ──────────────────────────────────────────────
resource "aws_sns_topic" "feedback" {
  name = "${local.prefix}-feedback-topic"
}

# ──────────────────────────────────────────────
# SQS Queue — consumed by Spring service
# ──────────────────────────────────────────────
resource "aws_sqs_queue" "feedback_dlq" {
  name                      = "${local.prefix}-feedback-dlq"
  message_retention_seconds = 1209600
}

resource "aws_sqs_queue" "feedback" {
  name                       = "${local.prefix}-feedback-queue"
  visibility_timeout_seconds = 60
  message_retention_seconds  = 345600
  receive_wait_time_seconds  = 5

  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.feedback_dlq.arn
    maxReceiveCount     = 3
  })
}

resource "aws_sqs_queue_policy" "feedback" {
  queue_url = aws_sqs_queue.feedback.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "sns.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.feedback.arn
      Condition = {
        ArnEquals = { "aws:SourceArn" = aws_sns_topic.feedback.arn }
      }
    }]
  })
}

# SNS → SQS subscription
resource "aws_sns_topic_subscription" "sqs" {
  topic_arn = aws_sns_topic.feedback.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.feedback.arn
}

# ──────────────────────────────────────────────
# DynamoDB Table — stores feedback items
# ──────────────────────────────────────────────
resource "aws_dynamodb_table" "feedback" {
  name         = "${local.prefix}-feedback"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"
  range_key    = "dataEnvio"

  attribute {
    name = "id"
    type = "S"
  }

  attribute {
    name = "dataEnvio"
    type = "S"
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

# ──────────────────────────────────────────────
# IAM — Lambda execution role
# ──────────────────────────────────────────────
resource "aws_iam_role" "lambda_exec" {
  name = "${local.prefix}-lambda-email-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "lambda.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "lambda_ses" {
  name = "${local.prefix}-lambda-ses-policy"
  role = aws_iam_role.lambda_exec.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect   = "Allow"
        Action   = ["ses:SendEmail", "ses:SendRawEmail"]
        Resource = "*"
      },
      {
        Effect   = "Allow"
        Action   = ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"]
        Resource = "arn:aws:logs:*:*:*"
      }
    ]
  })
}

# ──────────────────────────────────────────────
# Lambda — email notification for urgent feedback
# ──────────────────────────────────────────────
resource "aws_lambda_function" "email" {
  function_name = "${local.prefix}-email-handler"
  role          = aws_iam_role.lambda_exec.arn
  handler       = "com.feedback.lambda.EmailHandler::handleRequest"
  runtime       = "java21"
  memory_size   = var.lambda_memory_size
  timeout       = var.lambda_timeout

  s3_bucket = aws_s3_bucket.bucket.id
  s3_key    = "lambda/${var.environment}/lambda-email.jar"

  environment {
    variables = {
      SENDER_EMAIL = var.sender_email
      ADMIN_EMAIL  = var.admin_email
      AWS_REGION   = var.aws_region
    }
  }

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

resource "aws_lambda_permission" "sns" {
  statement_id  = "AllowSNSInvoke"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.email.function_name
  principal     = "sns.amazonaws.com"
  source_arn    = aws_sns_topic.feedback.arn
}

# SNS → Lambda subscription
resource "aws_sns_topic_subscription" "lambda" {
  topic_arn = aws_sns_topic.feedback.arn
  protocol  = "lambda"
  endpoint  = aws_lambda_function.email.arn

  filter_policy = jsonencode({
    urgencia = ["CRITICA", "ALTA"]
  })
}

# ──────────────────────────────────────────────
# IAM — EC2 instance profile
# ──────────────────────────────────────────────
resource "aws_iam_role" "ec2_role" {
  name = "${local.prefix}-ec2-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ec2.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy" "ec2_policy" {
  name = "${local.prefix}-ec2-policy"
  role = aws_iam_role.ec2_role.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sns:Publish"
        ]
        Resource = [aws_sns_topic.feedback.arn]
      },
      {
        Effect = "Allow"
        Action = [
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes"
        ]
        Resource = [aws_sqs_queue.feedback.arn]
      },
      {
        Effect = "Allow"
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:Scan",
          "dynamodb:Query"
        ]
        Resource = [aws_dynamodb_table.feedback.arn]
      }
    ]
  })
}

resource "aws_iam_instance_profile" "ec2" {
  name = "${local.prefix}-ec2-profile"
  role = aws_iam_role.ec2_role.name
}

# ──────────────────────────────────────────────
# Security Group
# ──────────────────────────────────────────────
resource "aws_security_group" "app" {
  name        = "${local.prefix}-app-sg"
  description = "Security group for feedback application EC2 instances"

  ingress {
    description = "HTTP Quarkus API"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTP Spring Service"
    from_port   = 8081
    to_port     = 8081
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name        = "${local.prefix}-app-sg"
    Project     = var.project_name
    Environment = var.environment
  }
}

# ──────────────────────────────────────────────
# EC2 — Quarkus API
# ──────────────────────────────────────────────
data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]

  filter {
    name   = "name"
    values = ["al2023-ami-*-x86_64"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

resource "aws_instance" "quarkus_api" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.ec2_instance_type
  key_name               = var.ec2_key_name != "" ? var.ec2_key_name : null
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  vpc_security_group_ids = [aws_security_group.app.id]

  user_data = base64encode(templatefile("${path.module}/templates/quarkus-userdata.sh", {
    aws_region    = var.aws_region
    sns_topic_arn = aws_sns_topic.feedback.arn
  }))

  tags = {
    Name        = "${local.prefix}-quarkus-api"
    Project     = var.project_name
    Environment = var.environment
  }
}

# ──────────────────────────────────────────────
# EC2 — Spring Service
# ──────────────────────────────────────────────
resource "aws_instance" "spring_service" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.ec2_instance_type
  key_name               = var.ec2_key_name != "" ? var.ec2_key_name : null
  iam_instance_profile   = aws_iam_instance_profile.ec2.name
  vpc_security_group_ids = [aws_security_group.app.id]

  user_data = base64encode(templatefile("${path.module}/templates/spring-userdata.sh", {
    aws_region     = var.aws_region
    sqs_queue_url  = aws_sqs_queue.feedback.url
    dynamodb_table = aws_dynamodb_table.feedback.name
  }))

  tags = {
    Name        = "${local.prefix}-spring-service"
    Project     = var.project_name
    Environment = var.environment
  }
}

# ──────────────────────────────────────────────
# CloudWatch — Lambda log group
# ──────────────────────────────────────────────
resource "aws_cloudwatch_log_group" "lambda" {
  name              = "/aws/lambda/${aws_lambda_function.email.function_name}"
  retention_in_days = 14

  tags = {
    Project     = var.project_name
    Environment = var.environment
  }
}

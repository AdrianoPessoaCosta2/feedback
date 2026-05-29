variable "bucket_name" {
  type = string
}

variable "project_name" {
  type    = string
  default = "feedback"
}

variable "environment" {
  type    = string
  default = "dev"
}

variable "aws_region" {
  type    = string
  default = "us-east-2"
}

variable "sender_email" {
  type        = string
  description = "SES verified email for sending notifications"
  default     = ""
}

variable "admin_email" {
  type        = string
  description = "Admin email for receiving urgent feedback alerts"
  default     = ""
}

variable "ec2_key_name" {
  type        = string
  description = "EC2 key pair name for SSH access"
  default     = ""
}

variable "ec2_instance_type" {
  type    = string
  default = "t3.micro"
}

variable "lambda_memory_size" {
  type    = number
  default = 512
}

variable "lambda_timeout" {
  type    = number
  default = 30
}

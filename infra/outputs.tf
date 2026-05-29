output "sns_topic_arn" {
  value = aws_sns_topic.feedback.arn
}

output "sqs_queue_url" {
  value = aws_sqs_queue.feedback.url
}

output "dynamodb_table_name" {
  value = aws_dynamodb_table.feedback.name
}

output "lambda_function_name" {
  value = aws_lambda_function.email.function_name
}

output "quarkus_api_public_ip" {
  value = aws_instance.quarkus_api.public_ip
}

output "spring_service_public_ip" {
  value = aws_instance.spring_service.public_ip
}

output "s3_bucket_name" {
  value = aws_s3_bucket.bucket.id
}

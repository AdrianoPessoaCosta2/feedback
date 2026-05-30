#!/bin/bash
set -euo pipefail
exec > /var/log/user-data.log 2>&1

yum update -y
yum install -y java-21-amazon-corretto

mkdir -p /opt/app

# Download application JAR from S3 (AWS CLI is pre-installed on AL2023)
aws s3 cp "s3://${s3_bucket}/apps/${environment}/service-spring.jar" /opt/app/app.jar
chown -R ec2-user:ec2-user /opt/app

# Create systemd service
cat > /etc/systemd/system/spring-service.service <<'EOF'
[Unit]
Description=Spring Feedback Service
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/app
Environment="AWS_REGION=${aws_region}"
Environment="SQS_QUEUE_URL=${sqs_queue_url}"
Environment="DYNAMODB_TABLE_NAME=${dynamodb_table}"
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable spring-service --now

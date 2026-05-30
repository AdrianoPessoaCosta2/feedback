#!/bin/bash
set -euo pipefail

yum update -y
yum install -y java-21-amazon-corretto aws-cli

mkdir -p /opt/app

# Download application JAR from S3
aws s3 cp "s3://${s3_bucket}/apps/${environment}/service-spring.jar" /opt/app/app.jar

cat > /etc/systemd/system/spring-service.service <<'EOF'
[Unit]
Description=Spring Feedback Service
After=network.target

[Service]
Type=simple
User=ec2-user
Environment="AWS_REGION=${aws_region}"
Environment="SQS_QUEUE_URL=${sqs_queue_url}"
Environment="DYNAMODB_TABLE_NAME=${dynamodb_table}"
ExecStart=/usr/bin/java -jar /opt/app/app.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

chown -R ec2-user:ec2-user /opt/app
systemctl daemon-reload
systemctl enable spring-service --now

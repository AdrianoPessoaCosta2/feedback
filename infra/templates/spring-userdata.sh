#!/bin/bash
set -euo pipefail

yum update -y
yum install -y java-21-amazon-corretto docker
systemctl enable docker --now
usermod -aG docker ec2-user

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

mkdir -p /opt/app
systemctl daemon-reload

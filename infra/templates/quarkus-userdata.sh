#!/bin/bash
set -euo pipefail
exec > /var/log/user-data.log 2>&1

yum update -y
yum install -y java-21-amazon-corretto

mkdir -p /opt/app

# Download application JAR from S3 (AWS CLI is pre-installed on AL2023)
aws s3 cp "s3://${s3_bucket}/apps/${environment}/api-quarkus.jar" /opt/app/quarkus-run.jar
chown -R ec2-user:ec2-user /opt/app

# Create systemd service
cat > /etc/systemd/system/quarkus-api.service <<'EOF'
[Unit]
Description=Quarkus Feedback API
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/app
Environment="AWS_REGION=${aws_region}"
Environment="SNS_TOPIC_ARN=${sns_topic_arn}"
ExecStart=/usr/bin/java -jar /opt/app/quarkus-run.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable quarkus-api --now

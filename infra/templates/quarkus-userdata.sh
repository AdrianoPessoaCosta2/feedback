#!/bin/bash
set -euo pipefail

yum update -y
yum install -y java-21-amazon-corretto aws-cli

mkdir -p /opt/app

# Download application JAR from S3
aws s3 cp "s3://${s3_bucket}/apps/${environment}/api-quarkus.jar" /opt/app/quarkus-run.jar

cat > /etc/systemd/system/quarkus-api.service <<'EOF'
[Unit]
Description=Quarkus Feedback API
After=network.target

[Service]
Type=simple
User=ec2-user
Environment="AWS_REGION=${aws_region}"
Environment="SNS_TOPIC_ARN=${sns_topic_arn}"
ExecStart=/usr/bin/java -jar /opt/app/quarkus-run.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

chown -R ec2-user:ec2-user /opt/app
systemctl daemon-reload
systemctl enable quarkus-api --now

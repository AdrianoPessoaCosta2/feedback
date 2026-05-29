#!/bin/bash
set -euo pipefail

yum update -y
yum install -y java-21-amazon-corretto docker
systemctl enable docker --now
usermod -aG docker ec2-user

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

mkdir -p /opt/app
systemctl daemon-reload

# Lookup Ubuntu 22.04 LTS AMI (only queried if ami_id is not explicitly provided)
data "aws_ami" "ubuntu" {
  count       = var.ami_id != "" ? 0 : 1
  most_recent = true
  owners      = ["099720109477"] # Canonical

  filter {
    name   = "name"
    values = ["ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*"]
  }

  filter {
    name   = "virtualization-type"
    values = ["hvm"]
  }
}

# IAM Role for EC2 Instances (CloudWatch & SSM)
resource "aws_iam_role" "ec2_role" {
  name = "pos-ec2-role-${var.environment}"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      }
    ]
  })
}

# Attach SSM Managed Instance Policy (allows session manager access without open port 22)
resource "aws_iam_role_policy_attachment" "ssm_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# Attach CloudWatch Logs Policy
resource "aws_iam_role_policy_attachment" "cloudwatch_policy" {
  role       = aws_iam_role.ec2_role.name
  policy_arn = "arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy"
}

resource "aws_iam_instance_profile" "ec2_profile" {
  name = "pos-ec2-profile-${var.environment}"
  role = aws_iam_role.ec2_role.name
}

# 1. Application Server EC2 (Spring Boot + Nginx)
resource "aws_instance" "app_server" {
  ami                  = var.ami_id != "" ? var.ami_id : data.aws_ami.ubuntu[0].id
  instance_type        = var.app_instance_type
  subnet_id            = var.public_subnet_id
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name
  key_name             = var.key_name

  vpc_security_group_ids = [
    var.web_security_group_id,
    var.app_security_group_id
  ]

  root_block_device {
    volume_size           = 20
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  tags = {
    Name = "pos-app-server-${var.environment}"
    Role = "Application"
  }
}

# 2. CI/CD Automation Server EC2 (Jenkins + SonarQube)
resource "aws_instance" "jenkins_server" {
  ami                  = var.ami_id != "" ? var.ami_id : data.aws_ami.ubuntu[0].id
  instance_type        = var.jenkins_instance_type
  subnet_id            = var.public_subnet_id
  iam_instance_profile = aws_iam_instance_profile.ec2_profile.name
  key_name             = var.key_name

  vpc_security_group_ids = [
    var.jenkins_security_group_id
  ]

  user_data = <<-EOF
              #!/bin/bash
              # Allocate 4GB swap space to enable running Jenkins + SonarQube on Free-Tier t3.micro
              if [ ! -f /swapfile ]; then
                fallocate -l 4G /swapfile || dd if=/dev/zero of=/swapfile bs=1M count=4096
                chmod 600 /swapfile
                mkswap /swapfile
                swapon /swapfile
                echo '/swapfile none swap sw 0 0' >> /etc/fstab
                sysctl vm.swappiness=10
                echo 'vm.swappiness=10' >> /etc/sysctl.conf
              fi
              EOF

  root_block_device {
    volume_size           = 30
    volume_type           = "gp3"
    delete_on_termination = true
    encrypted             = true
  }

  tags = {
    Name = "pos-jenkins-server-${var.environment}"
    Role = "CICD"
  }
}

# 1. Public Nginx / Web Entry Security Group
resource "aws_security_group" "web" {
  name        = "pos-web-sg-${var.environment}"
  description = "Allows public HTTP/HTTPS traffic to Nginx reverse proxy"
  vpc_id      = var.vpc_id

  ingress {
    description = "HTTP Public"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS Public"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH from approved IP"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_ip]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "pos-web-sg-${var.environment}"
  }
}

# 2. Application Layer Security Group (Spring Boot)
resource "aws_security_group" "app" {
  name        = "pos-app-sg-${var.environment}"
  description = "Allows traffic from Nginx and management SSH"
  vpc_id      = var.vpc_id

  ingress {
    description     = "Spring Boot API from Nginx Web SG"
    from_port       = 8080
    to_port         = 8080
    protocol        = "tcp"
    security_groups = [aws_security_group.web.id]
  }

  ingress {
    description = "SSH from approved IP / Bastion"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_ip]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "pos-app-sg-${var.environment}"
  }
}

# 3. Database Layer Security Group (RDS MySQL)
resource "aws_security_group" "rds" {
  name        = "pos-rds-sg-${var.environment}"
  description = "Restricts MySQL access strictly to the Application Layer"
  vpc_id      = var.vpc_id

  ingress {
    description     = "MySQL from App Instances"
    from_port       = 3306
    to_port         = 3306
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    description = "No outbound required for RDS"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "pos-rds-sg-${var.environment}"
  }
}

# 4. CI/CD Security Group (Jenkins & SonarQube)
resource "aws_security_group" "jenkins" {
  name        = "pos-jenkins-sg-${var.environment}"
  description = "Allows Jenkins and SonarQube dashboard access from admin IP"
  vpc_id      = var.vpc_id

  ingress {
    description = "Jenkins Web UI"
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_ip]
  }

  ingress {
    description = "SonarQube Web UI"
    from_port   = 9000
    to_port     = 9000
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_ip]
  }

  ingress {
    description = "SSH Management"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = [var.ssh_allowed_ip]
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  tags = {
    Name = "pos-jenkins-sg-${var.environment}"
  }
}

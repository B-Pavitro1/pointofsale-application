variable "aws_region" {
  description = "AWS deployment region"
  type        = string
  default     = "ap-southeast-2"
}

variable "environment" {
  description = "Deployment environment (dev, staging, prod)"
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "Availability zones list"
  type        = list(string)
  default     = ["ap-southeast-2a", "ap-southeast-2b"]
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets (Nginx, Jenkins)"
  type        = list(string)
  default     = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private app subnets (Spring Boot)"
  type        = list(string)
  default     = ["10.0.10.0/24", "10.0.11.0/24"]
}

variable "private_db_subnet_cidrs" {
  description = "CIDR blocks for private database subnets (RDS MySQL)"
  type        = list(string)
  default     = ["10.0.20.0/24", "10.0.21.0/24"]
}

variable "ssh_allowed_ip" {
  description = "Public IP address or CIDR permitted to SSH and access Jenkins UI (e.g. your IP/32)"
  type        = string
  default     = "0.0.0.0/0" # In real production, restrict strictly to user IP
}

variable "app_instance_type" {
  description = "EC2 instance type for POS App server"
  type        = string
  default     = "t3.micro"
}

variable "jenkins_instance_type" {
  description = "EC2 instance type for Jenkins CI & SonarQube"
  type        = string
  default     = "t3.medium"
}

variable "key_name" {
  description = "AWS EC2 Key Pair name for SSH authentication"
  type        = string
  default     = "pos-deployer-key"
}

# RDS Configuration
variable "db_instance_class" {
  description = "RDS DB instance class (db.t3.micro is AWS Free Tier eligible)"
  type        = string
  default     = "db.t3.micro"
}

variable "db_allocated_storage" {
  description = "Allocated storage in GB for RDS"
  type        = number
  default     = 20
}

variable "db_name" {
  description = "Initial MySQL database name"
  type        = string
  default     = "posdb"
}

variable "db_username" {
  description = "Master MySQL database username"
  type        = string
  default     = "posadmin"
}

variable "db_password" {
  description = "Master MySQL database password (minimum 8 chars)"
  type        = string
  sensitive   = true
  default     = "PosSecurePassword123!"
}

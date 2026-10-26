# VPC Resource
resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "pos-vpc-${var.environment}"
  }
}

# Internet Gateway
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "pos-igw-${var.environment}"
  }
}

# Public Subnets (AZ-a, AZ-b)
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = {
    Name = "pos-public-subnet-${count.index + 1}-${var.environment}"
    Tier = "Public"
  }
}

# Private App Subnets (AZ-a, AZ-b)
resource "aws_subnet" "private_app" {
  count             = length(var.private_app_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_app_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "pos-private-app-subnet-${count.index + 1}-${var.environment}"
    Tier = "Application"
  }
}

# Private Database Subnets (AZ-a, AZ-b)
resource "aws_subnet" "private_db" {
  count             = length(var.private_db_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_db_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = {
    Name = "pos-private-db-subnet-${count.index + 1}-${var.environment}"
    Tier = "Database"
  }
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "pos-public-rt-${var.environment}"
  }
}

# Public Route Table Associations
resource "aws_route_table_association" "public" {
  count          = length(var.public_subnet_cidrs)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# Private Database Subnet Group for RDS
resource "aws_db_subnet_group" "rds" {
  name       = "pos-rds-subnet-group-${var.environment}"
  subnet_ids = aws_subnet.private_db[*].id

  tags = {
    Name = "pos-rds-subnet-group-${var.environment}"
  }
}

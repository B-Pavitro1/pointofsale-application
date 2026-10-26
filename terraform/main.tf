# 1. Networking Module (VPC, Subnets, Gateways, Route Tables)
module "vpc" {
  source                   = "./modules/vpc"
  environment              = var.environment
  vpc_cidr                 = var.vpc_cidr
  availability_zones       = var.availability_zones
  public_subnet_cidrs      = var.public_subnet_cidrs
  private_app_subnet_cidrs = var.private_app_subnet_cidrs
  private_db_subnet_cidrs  = var.private_db_subnet_cidrs
}

# 2. Security Groups Module (Least-Privilege Traffic Rules)
module "security_groups" {
  source         = "./modules/security_groups"
  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  ssh_allowed_ip = var.ssh_allowed_ip
}

# 3. Compute Module (EC2 for POS App and Jenkins CI/CD)
module "ec2" {
  source                    = "./modules/ec2"
  environment               = var.environment
  public_subnet_id          = module.vpc.public_subnet_ids[0]
  web_security_group_id     = module.security_groups.web_security_group_id
  app_security_group_id     = module.security_groups.app_security_group_id
  jenkins_security_group_id = module.security_groups.jenkins_security_group_id
  app_instance_type         = var.app_instance_type
  jenkins_instance_type     = var.jenkins_instance_type
  key_name                  = var.key_name
}

# 4. Database Module (Amazon RDS MySQL in Private DB Subnet)
module "rds" {
  source                = "./modules/rds"
  environment           = var.environment
  db_subnet_group_name  = module.vpc.db_subnet_group_name
  rds_security_group_id = module.security_groups.rds_security_group_id
  db_instance_class     = var.db_instance_class
  db_allocated_storage  = var.db_allocated_storage
  db_name               = var.db_name
  db_username           = var.db_username
  db_password           = var.db_password
}

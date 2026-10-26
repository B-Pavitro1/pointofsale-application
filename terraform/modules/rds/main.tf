resource "aws_db_instance" "mysql" {
  identifier        = "pos-mysql-${var.environment}"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = var.db_instance_class
  allocated_storage = var.db_allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true

  db_name  = var.db_name
  username = var.db_username
  password = var.db_password

  db_subnet_group_name   = var.db_subnet_group_name
  vpc_security_group_ids = [var.rds_security_group_id]

  publicly_accessible = false
  multi_az            = false # Single AZ for free-tier / demo economy
  skip_final_snapshot = true  # Set to true for disposable portfolio environment
  deletion_protection = false

  auto_minor_version_upgrade = true
  backup_retention_period    = 1 # 1 day backup for demo

  tags = {
    Name = "pos-rds-mysql-${var.environment}"
  }
}

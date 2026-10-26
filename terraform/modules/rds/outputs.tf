output "rds_endpoint" {
  description = "Connection endpoint for Amazon RDS MySQL"
  value       = aws_db_instance.mysql.endpoint
}

output "rds_address" {
  description = "Hostname of RDS MySQL instance"
  value       = aws_db_instance.mysql.address
}

output "rds_port" {
  description = "Port of RDS MySQL instance"
  value       = aws_db_instance.mysql.port
}

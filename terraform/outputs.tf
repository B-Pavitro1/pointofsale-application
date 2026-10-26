output "vpc_id" {
  description = "The ID of the provisioned VPC"
  value       = module.vpc.vpc_id
}

output "pos_app_public_ip" {
  description = "Public IP for accessing the Point of Sale Web UI"
  value       = module.ec2.app_server_public_ip
}

output "jenkins_public_ip" {
  description = "Public IP of the Jenkins & SonarQube CI/CD Server"
  value       = module.ec2.jenkins_server_public_ip
}

output "jenkins_url" {
  description = "Web address for Jenkins Dashboard"
  value       = "http://${module.ec2.jenkins_server_public_ip}:8080"
}

output "sonarqube_url" {
  description = "Web address for SonarQube Quality Gate Dashboard"
  value       = "http://${module.ec2.jenkins_server_public_ip}:9000"
}

output "rds_mysql_endpoint" {
  description = "Private connection endpoint for RDS MySQL"
  value       = module.rds.rds_endpoint
}

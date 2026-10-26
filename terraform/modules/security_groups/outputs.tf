output "web_security_group_id" {
  value = aws_security_group.web.id
}

output "app_security_group_id" {
  value = aws_security_group.app.id
}

output "rds_security_group_id" {
  value = aws_security_group.rds.id
}

output "jenkins_security_group_id" {
  value = aws_security_group.jenkins.id
}

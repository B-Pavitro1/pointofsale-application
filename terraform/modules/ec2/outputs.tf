output "app_server_id" {
  value = aws_instance.app_server.id
}

output "app_server_public_ip" {
  value = aws_instance.app_server.public_ip
}

output "app_server_private_ip" {
  value = aws_instance.app_server.private_ip
}

output "jenkins_server_id" {
  value = aws_instance.jenkins_server.id
}

output "jenkins_server_public_ip" {
  value = aws_instance.jenkins_server.public_ip
}

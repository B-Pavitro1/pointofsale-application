variable "environment" { type = string }
variable "public_subnet_id" { type = string }
variable "app_security_group_id" { type = string }
variable "web_security_group_id" { type = string }
variable "jenkins_security_group_id" { type = string }
variable "app_instance_type" { type = string }
variable "jenkins_instance_type" { type = string }
variable "key_name" { type = string }
variable "ami_id" {
  description = "Optional AMI ID. If empty, dynamically looks up Ubuntu 22.04 LTS AMI."
  type        = string
  default     = ""
}

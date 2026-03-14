output "cluster_name" {
  description = "Nom du cluster EKS"
  value       = module.eks.cluster_name
}

output "cluster_endpoint" {
  description = "Endpoint de l'API Kubernetes"
  value       = module.eks.cluster_endpoint
}

output "cluster_version" {
  description = "Version de Kubernetes"
  value       = module.eks.cluster_version
}

output "kubeconfig_command" {
  description = "Commande pour configurer kubectl"
  value       = "aws eks update-kubeconfig --region ${var.aws_region} --name ${module.eks.cluster_name}"
}

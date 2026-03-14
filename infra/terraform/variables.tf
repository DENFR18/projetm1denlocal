variable "aws_region" {
  description = "Région AWS où déployer le cluster EKS"
  type        = string
  default     = "eu-west-1"
}

variable "cluster_name" {
  description = "Nom du cluster EKS"
  type        = string
  default     = "projetm1-eks"
}

variable "node_instance_type" {
  description = "Type d'instance EC2 pour les workers Kubernetes"
  type        = string
  default     = "t3.small"
}

variable "desired_nodes" {
  description = "Nombre de noeuds workers souhaités"
  type        = number
  default     = 2
}

variable "min_nodes" {
  description = "Nombre minimum de noeuds workers"
  type        = number
  default     = 1
}

variable "max_nodes" {
  description = "Nombre maximum de noeuds workers (autoscaling)"
  type        = number
  default     = 4
}

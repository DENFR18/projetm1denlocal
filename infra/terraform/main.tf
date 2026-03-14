locals {
  azs = slice(data.aws_availability_zones.available.names, 0, 2)
}

data "aws_availability_zones" "available" {
  filter {
    name   = "opt-in-status"
    values = ["opt-in-not-required"]
  }
}

# ─── VPC ────────────────────────────────────────────────────────────────────
module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "~> 5.0"

  name = "${var.cluster_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = local.azs
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24"]

  enable_nat_gateway   = true
  single_nat_gateway   = true  # 1 seule NAT Gateway pour réduire les coûts
  enable_dns_hostnames = true

  # Tags requis par EKS pour la découverte des subnets
  public_subnet_tags = {
    "kubernetes.io/role/elb" = 1
  }
  private_subnet_tags = {
    "kubernetes.io/role/internal-elb" = 1
  }

  tags = {
    Project     = "ProjetM1"
    Environment = "production"
  }
}

# ─── EKS Cluster ─────────────────────────────────────────────────────────────
module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "~> 20.0"

  cluster_name    = var.cluster_name
  cluster_version = "1.29"

  vpc_id                         = module.vpc.vpc_id
  subnet_ids                     = module.vpc.private_subnets
  cluster_endpoint_public_access = true

  # Managed Node Group (workers EC2 gérés par AWS)
  eks_managed_node_groups = {
    workers = {
      name           = "${var.cluster_name}-workers"
      instance_types = [var.node_instance_type]

      min_size     = var.min_nodes
      max_size     = var.max_nodes
      desired_size = var.desired_nodes

      labels = {
        role = "worker"
      }

      tags = {
        Project = "ProjetM1"
      }
    }
  }

  # Accès admin au cluster pour le compte Terraform
  enable_cluster_creator_admin_permissions = true

  tags = {
    Project     = "ProjetM1"
    Environment = "production"
  }
}

# Java 3-Tier Docker - Bastion + Private Servers

Architecture:

Internet -> Application Load Balancer -> Private Frontend -> Private Backend -> Private Database

SSH:
Your Laptop -> Bastion -> Private Frontend / Backend / Database

The Bastion is SSH-only. It is NOT part of application traffic.

Private servers:
1. Frontend EC2: Nginx + HTML/JS
2. Backend EC2: Java 17 + Spring Boot
3. Database EC2: MySQL 8

Dockerfiles are included for all three tiers.

## Application ports

Frontend container: 80
Backend container: 8080
Database container: 3306

## Server-to-server addresses

Frontend nginx needs:
BACKEND_PRIVATE_IP

Backend needs:
DB_PRIVATE_IP

Do not use 127.0.0.1 or localhost between different EC2 servers.

See DEPLOYMENT_STEPS.md for the exact AWS, Docker, Bastion, Security Group and ALB steps.

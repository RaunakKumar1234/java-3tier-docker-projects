# FULL DEPLOYMENT - AWS 3 PRIVATE SERVERS + 1 BASTION

## Final architecture

Internet
  |
  v
Application Load Balancer (public)
  |
  v
Frontend EC2 (private) :80
  |
  v
Backend EC2 (private) :8080
  |
  v
Database EC2 (private) :3306

SSH:
Laptop -> Bastion -> private servers

The Bastion is only for SSH.

IMPORTANT:
Replace every placeholder such as <BASTION_PUBLIC_IP>, <FRONTEND_PRIVATE_IP>, <BACKEND_PRIVATE_IP>, <DB_PRIVATE_IP>, and <DOCKERHUB_USERNAME> with your actual values.

--------------------------------------------------
PART A - SECURITY GROUPS
--------------------------------------------------

Create four Security Groups.

1) Bastion-SG
Inbound:
SSH TCP 22 -> My IP only

2) Frontend-SG
Inbound:
HTTP TCP 80 -> Application Load Balancer Security Group
SSH TCP 22 -> Bastion-SG

3) Backend-SG
Inbound:
TCP 8080 -> Frontend-SG
SSH TCP 22 -> Bastion-SG

4) Database-SG
Inbound:
TCP 3306 -> Backend-SG
SSH TCP 22 -> Bastion-SG

Do NOT open 3306 to 0.0.0.0/0.
Do NOT open 8080 to 0.0.0.0/0.

For the ALB, create an ALB Security Group:
Inbound:
HTTP TCP 80 -> 0.0.0.0/0

Frontend-SG should allow port 80 from ALB-SG only.

--------------------------------------------------
PART B - BASTION HOST
--------------------------------------------------

SSH from your laptop:

ssh -i your-key.pem ubuntu@<BASTION_PUBLIC_IP>

Check:

hostname
ip addr

The Bastion does not need Docker for this project.

To connect to Frontend:

ssh -i your-key.pem ubuntu@<FRONTEND_PRIVATE_IP>

If your private key is on your laptop and the Bastion is configured as a jump host, use:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<FRONTEND_PRIVATE_IP>

Backend:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<BACKEND_PRIVATE_IP>

Database:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<DB_PRIVATE_IP>

If you use ProxyJump, the private servers need no public IP.

--------------------------------------------------
PART C - DATABASE SERVER
--------------------------------------------------

Connect:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<DB_PRIVATE_IP>

Install Docker:

sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker

Verify:

docker --version
docker compose version

Login:

docker login

Create deployment directory:

mkdir -p ~/java3tier-database
cd ~/java3tier-database

Create .env:

nano .env

Use:

DOCKERHUB_USERNAME=<DOCKERHUB_USERNAME>
MYSQL_ROOT_PASSWORD=<STRONG_ROOT_PASSWORD>
DB_NAME=productdb
DB_USER=appuser
DB_PASSWORD=<STRONG_DB_PASSWORD>

Create docker-compose.yml:

nano docker-compose.yml

Paste:

services:
  database:
    image: ${DOCKERHUB_USERNAME}/java3tier-database:latest
    container_name: java3tier-database
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${DB_NAME}
      MYSQL_USER: ${DB_USER}
      MYSQL_PASSWORD: ${DB_PASSWORD}
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql

volumes:
  mysql_data:

Pull and start:

docker compose pull
docker compose up -d

Check:

docker ps
docker logs java3tier-database

IMPORTANT:
The database is still protected by the Database-SG. Only Backend-SG can reach port 3306.

--------------------------------------------------
PART D - BACKEND SERVER
--------------------------------------------------

Connect:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<BACKEND_PRIVATE_IP>

Install Docker:

sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker

Verify:

docker --version
docker compose version

Login:

docker login

Create directory:

mkdir -p ~/java3tier-backend
cd ~/java3tier-backend

Create .env:

nano .env

Use:

DOCKERHUB_USERNAME=<DOCKERHUB_USERNAME>
DB_PRIVATE_IP=<DB_PRIVATE_IP>
DB_NAME=productdb
DB_USER=appuser
DB_PASSWORD=<STRONG_DB_PASSWORD>

Create docker-compose.yml:

nano docker-compose.yml

Paste:

services:
  backend:
    image: ${DOCKERHUB_USERNAME}/java3tier-backend:latest
    container_name: java3tier-backend
    restart: unless-stopped
    environment:
      DB_HOST: ${DB_PRIVATE_IP}
      DB_PORT: 3306
      DB_NAME: ${DB_NAME}
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"

Start:

docker compose pull
docker compose up -d

Check:

docker ps
docker logs java3tier-backend

Test local backend:

curl http://localhost:8080/api/products

Test DB connectivity from Backend:

sudo apt install -y netcat-openbsd
nc -vz <DB_PRIVATE_IP> 3306

Expected:
Connection to <DB_PRIVATE_IP> 3306 port [tcp/mysql] succeeded!

If it fails, check Database-SG and make sure TCP 3306 source is Backend-SG.

--------------------------------------------------
PART E - FRONTEND SERVER
--------------------------------------------------

Connect:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<FRONTEND_PRIVATE_IP>

Install Docker:

sudo apt update
sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker

Login:

docker login

Create directory:

mkdir -p ~/java3tier-frontend
cd ~/java3tier-frontend

Create .env:

nano .env

Use:

DOCKERHUB_USERNAME=<DOCKERHUB_USERNAME>
BACKEND_PRIVATE_IP=<BACKEND_PRIVATE_IP>

Create docker-compose.yml:

nano docker-compose.yml

Paste:

services:
  frontend:
    image: ${DOCKERHUB_USERNAME}/java3tier-frontend:latest
    container_name: java3tier-frontend
    restart: unless-stopped
    environment:
      BACKEND_PRIVATE_IP: ${BACKEND_PRIVATE_IP}
    ports:
      - "80:80"

Start:

docker compose pull
docker compose up -d

Check:

docker ps
docker logs java3tier-frontend

IMPORTANT:
The frontend image uses Nginx. The Nginx config in the image is generated at container startup from BACKEND_PRIVATE_IP.

Test Frontend -> Backend:

curl http://<BACKEND_PRIVATE_IP>:8080/api/products

Then test through Nginx:

curl http://localhost/api/products

The second command should return the same API JSON.

--------------------------------------------------
PART F - BUILD IMAGES
--------------------------------------------------

Build from a machine with the complete project:

Backend:

docker build -t java3tier-backend:latest ./backend

Frontend:

docker build -t java3tier-frontend:latest ./frontend

Database:

docker build -t java3tier-database:latest ./database

Check:

docker images

--------------------------------------------------
PART G - PUSH TO DOCKER HUB
--------------------------------------------------

Login:

docker login

Tag:

docker tag java3tier-backend:latest <DOCKERHUB_USERNAME>/java3tier-backend:latest
docker tag java3tier-frontend:latest <DOCKERHUB_USERNAME>/java3tier-frontend:latest
docker tag java3tier-database:latest <DOCKERHUB_USERNAME>/java3tier-database:latest

Push:

docker push <DOCKERHUB_USERNAME>/java3tier-backend:latest
docker push <DOCKERHUB_USERNAME>/java3tier-frontend:latest
docker push <DOCKERHUB_USERNAME>/java3tier-database:latest

Then the three private servers only need docker pull.

--------------------------------------------------
PART H - APPLICATION LOAD BALANCER
--------------------------------------------------

Create an Application Load Balancer.

Scheme:
Internet-facing

Listeners:
HTTP :80

Availability Zones:
Use subnets in the VPC.

Create Target Group:
Type: Instances
Protocol: HTTP
Port: 80
Health check path: /

Register the Frontend private EC2 instance.

Target should become healthy.

ALB Security Group:
Inbound TCP 80 from 0.0.0.0/0

Frontend Security Group:
Inbound TCP 80 from ALB-SG

Do NOT expose Frontend TCP 80 directly to the internet.

Open the ALB DNS name in a browser:

http://<ALB_DNS_NAME>

The browser traffic is:

Browser -> ALB -> Frontend -> Backend -> Database

--------------------------------------------------
PART I - FINAL TESTING
--------------------------------------------------

1. Database:

docker ps

2. Backend:

curl http://localhost:8080/api/products

3. From Frontend:

curl http://<BACKEND_PRIVATE_IP>:8080/api/products

4. Through Frontend Nginx:

curl http://localhost/api/products

5. From browser:

http://<ALB_DNS_NAME>

6. Add a product from the UI.

7. Refresh.

8. Verify the product remains after frontend/backend container restart.

--------------------------------------------------
PART J - TROUBLESHOOTING
--------------------------------------------------

Backend container stops:

docker logs java3tier-backend

Common cause:
DB_PRIVATE_IP or DB credentials are wrong.

Backend cannot reach MySQL:

nc -vz <DB_PRIVATE_IP> 3306

Check:
Database-SG -> 3306 from Backend-SG.

Frontend cannot reach Backend:

curl http://<BACKEND_PRIVATE_IP>:8080/api/products

Check:
Backend-SG -> 8080 from Frontend-SG.

ALB target unhealthy:

Check:
Frontend container is running:

docker ps

Check:

curl http://localhost/

ALB target group:
Protocol HTTP
Port 80
Health path /

SSH issue:

Use Bastion as ProxyJump:

ssh -i your-key.pem -J ubuntu@<BASTION_PUBLIC_IP> ubuntu@<PRIVATE_IP>

--------------------------------------------------
PART K - IMPORTANT DOCKER RULE
--------------------------------------------------

Do not use:

127.0.0.1
localhost

for communication between the three EC2 servers.

Correct:

Frontend -> Backend private IP
Backend -> Database private IP

Example:

Frontend:
BACKEND_PRIVATE_IP=10.0.2.20

Backend:
DB_PRIVATE_IP=10.0.3.20

127.0.0.1 is only correct when the service is in the same network namespace/container host where that service is actually running.

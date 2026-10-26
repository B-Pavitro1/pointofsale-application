# Production-Style 3-Tier Point of Sale (POS) Application & Automated CI/CD Pipeline

A production-grade, 3-tier retail Point of Sale (POS) application built with Java Spring Boot 3, MySQL 8.0, and a responsive HTML5/Bootstrap presentation tier. The application is deployed onto AWS using Infrastructure as Code (Terraform), server configuration management (Ansible), and an 8-stage automated CI/CD pipeline orchestrated by Jenkins and gated by SonarQube.

---

## 1. System Architecture

```
+-----------------------------------------------------------------------------------+
|                            AWS CLOUD CUSTOM VPC (10.0.0.0/16)                     |
|                                                                                   |
|   +---------------------------------------------------------------------------+   |
|   | Public Subnet (AZ-a): Internet Gateway                                    |   |
|   |  - Nginx Reverse Proxy (Port 80/443) -> Serves HTML/JS Frontend UI        |   |
|   |  - Jenkins & SonarQube CI/CD Server (Port 8080/9000 - IP Restricted)      |   |
|   +---------------------------------------------------------------------------+   |
|                                     |                                             |
|                                     v (Internal HTTP Proxy: 8080)                 |
|   +---------------------------------------------------------------------------+   |
|   | Private App Subnet (AZ-a):                                                |   |
|   |  - Spring Boot 3 POS REST API (Port 8080 - Systemd / Docker)              |   |
|   |  - Stateless JWT Security, Role-Based Access Control                      |   |
|   |  - Pessimistic Locking on Checkout to Prevent Stock Overselling          |   |
|   +---------------------------------------------------------------------------+   |
|                                     |                                             |
|                                     v (JDBC Port 3306 - Private SG Only)          |
|   +---------------------------------------------------------------------------+   |
|   | Private DB Subnet (AZ-a & AZ-b):                                          |   |
|   |  - Amazon RDS for MySQL 8.0 (Encrypted at Rest, No Public Route)          |   |
|   |  - Automated Schema Management via Spring Data JPA / Hibernate            |   |
|   +---------------------------------------------------------------------------+   |
+-----------------------------------------------------------------------------------+
```

### Logical Tiers
- **Tier 1 — Presentation Layer**: HTML5, CSS3, Vanilla JavaScript, and Bootstrap 5. Provides a responsive POS cash register, real-time cart, checkout modal, inventory tracker with low-stock badges, and sales reporting.
- **Tier 2 — Application Layer**: Spring Boot 3 (Java 21 LTS), Spring Security with stateless JWT, Role-Based Access Control (`ADMIN`, `CASHIER`, `INVENTORY_MANAGER`), centralized exception handling (`@RestControllerAdvice`), and pessimistic locking (`PESSIMISTIC_WRITE`) to ensure consistent checkout transactions.
- **Tier 3 — Database Layer**: MySQL 8.0 with normalized schema (`users`, `roles`, `products`, `inventory`, `sales`, `sale_items`, `payments`), foreign key constraints, and SQL initialization scripts.

---

## 2. Directory Structure

```
pointofsale-application/
├── backend/                       # Tier 2: Spring Boot 3 Application
│   ├── src/main/java/com/pos/     # Controllers, Services, Repositories, Entities, Security
│   ├── src/main/resources/        # application.yml, db/migration/ (Flyway SQL scripts)
│   ├── src/test/java/com/pos/     # Unit & Integration Tests (100% Pass)
│   ├── Dockerfile                 # Multi-stage container build (temurin-21-jre-alpine)
│   └── pom.xml                    # Maven configuration (JaCoCo + SonarQube)
├── frontend/                      # Tier 1: Presentation Layer
│   ├── index.html                 # POS Register, Inventory, Sales, Dashboard UI
│   ├── css/style.css              # Custom styling & thermal receipt styles
│   ├── js/app.js                  # REST API Client with JWT authorization
│   ├── Dockerfile                 # Nginx container
│   └── nginx.conf                 # Nginx reverse proxy routing
├── terraform/                     # Infrastructure as Code
│   ├── environments/dev/          # Environment tfvars
│   ├── modules/                   # Modular VPC, EC2, RDS, Security Groups
│   ├── main.tf, variables.tf, outputs.tf
│   └── versions.tf
├── ansible/                       # Configuration Management
│   ├── inventory/                 # staging.ini, prod.ini
│   ├── playbooks/                 # setup-ci-server.yml, setup-app-server.yml, deploy-app.yml
│   └── ansible.cfg
├── jenkins/                       # CI/CD Pipeline
│   └── Jenkinsfile                # 8-Stage Declarative Pipeline
├── docker/                        # Local Development
│   └── docker-compose.yml         # MySQL 8.0 + Backend + Frontend
├── .gitignore
├── .env.example
└── README.md
```

---

## 3. Local Development Quickstart

### Option A: Run Full Stack via Docker Compose (Fastest)

```bash
cd docker
docker compose up -d --build
```
- Frontend POS UI: `http://localhost:3000`
- Spring Boot REST API: `http://localhost:8080/api/v1/products`
- Spring Actuator Health: `http://localhost:8080/actuator/health`

### Option B: Run Spring Boot Locally with Local MySQL

1. **Start MySQL container**:
   ```bash
   cd docker
   docker compose up -d mysql
   ```
2. **Run Spring Boot**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
3. **Open Frontend**:
   Simply open `frontend/index.html` in your browser.

### Default Seed Accounts
| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `admin` | `password123` | `ROLE_ADMIN` | Full access (POS, Stock, Sales, Reports) |
| `cashier` | `password123` | `ROLE_CASHIER` | POS Register, Cart, Checkout, Sales History |
| `manager` | `password123` | `ROLE_INVENTORY_MANAGER` | Product CRUD, Stock Adjustments, Low Stock Alerts |

---

## 4. Running Tests & Code Coverage

Execute the automated test suite with JaCoCo coverage analysis:

```bash
cd backend
mvn clean test
```

### Test Coverage Highlights
- **Checkout Concurrency & Overselling Prevention**: Verified with pessimistic locking tests.
- **Stock Deductions**: Verified atomic inventory balance updates.
- **Security Validation**: Verified unauthorized/forbidden access rejections and JWT expiration.
- **Validation Constraints**: Verified rejection of negative stock, missing fields, and bad credentials.

---

## 5. AWS Infrastructure as Code (Terraform)

### Commands

```bash
cd terraform

# 1. Format configuration
terraform fmt

# 2. Initialize modules and AWS provider
terraform init

# 3. Validate syntax and resource schema
terraform validate

# 4. Preview infrastructure plan
terraform plan -var-file="environments/dev/terraform.tfvars.example"

# 5. Provision AWS infrastructure
terraform apply -var-file="environments/dev/terraform.tfvars.example" -auto-approve

# 6. Teardown when finished to avoid AWS charges
terraform destroy -var-file="environments/dev/terraform.tfvars.example" -auto-approve
```

---

## 6. Server Configuration (Ansible)

Execute idempotent playbooks to prepare target EC2 instances:

```bash
cd ansible

# 1. Verify connectivity to servers
ansible all -m ping

# 2. Configure CI Server (Jenkins, SonarQube, Maven, Docker)
ansible-playbook playbooks/setup-ci-server.yml

# 3. Configure Application Server (Java 21, Nginx, Systemd)
ansible-playbook playbooks/setup-app-server.yml

# 4. Deploy Application Artifact & Run Health Verification
ansible-playbook playbooks/deploy-app.yml
```

---

## 7. Jenkins CI/CD Pipeline (8 Stages)

The pipeline automates delivery from code commit to production verification:

```
[Git SCM] ──> [Build (Maven)] ──> [Automated Tests (JUnit)] ──> [SonarQube Quality Gate]
     │
     └──> [Artifact Repo (SHA-256 Checksum)] ──> [Staging Deploy (Ansible)] ──> [Manual Approval] ──> [Production Deploy] ──> [Monitoring & Health]
```

### Key Security Safeguards
1. **Pessimistic Locking**: Prevents race conditions during simultaneous checkouts.
2. **Stateless JWT**: No server-side sessions, eliminating session fixation vulnerabilities.
3. **Least Privilege Networking**: RDS resides strictly in private subnets with port 3306 open only to the application security group.
4. **Non-Root Execution**: Docker container and systemd service execute under a dedicated unprivileged user (`posapp` / `appuser`).
5. **No Card Data Storage**: PCI-DSS compliant mock payment tokenization.
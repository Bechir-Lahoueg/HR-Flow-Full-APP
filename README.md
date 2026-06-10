# HR-Flow

**HR-Flow** is an integrated Human Resources management platform built as a monorepo.
It combines a full **Symfony web application** and a modular **JavaFX desktop application**,
both backed by a shared MySQL database. The platform covers recruitment, leave management,
training, employee relations, compensation, user management, and AI-assisted reporting.

> A personal project designed and developed by **Bechir Lahoueg** — [bechirlahoueg.tech](https://bechirlahoueg.tech).

---

## 📁 Monorepo Structure

```
HR-Flow-Full-APP/
├── HrFlow-Web/          # Symfony 6.4 web application (PHP, Twig, Tailwind, Docker)
│   ├── src/             # Application code (Controllers, Entities, Services…)
│   ├── templates/       # Twig templates
│   ├── assets/          # Front-end assets (Stimulus, Tailwind sources)
│   ├── config/          # Symfony configuration
│   ├── migrations/      # Doctrine migrations
│   ├── public/          # Web root (index.php, compiled assets)
│   └── .env.example     # Web environment variable template
│
├── HrFlow-Desktop/      # JavaFX 21 multi-module desktop application (Java 17, Maven)
│   ├── AppUi/           # JavaFX entry point that aggregates all business modules
│   ├── AiServices/      # AI / reporting / CSV import-export / data visualization
│   ├── GestionUtilisateur/        # User management
│   ├── GestionDesConges/          # Leave management
│   ├── GestionRecrutement/        # Recruitment
│   ├── Gestionformation/          # Training
│   ├── GestionRelationEmployees/  # Employee relations
│   ├── GestionRemuneration/       # Compensation / payroll
│   └── .env.example     # Desktop environment variable template
│
├── README.md            # ← you are here
└── .gitignore           # Root-level ignore rules
```

---

## 🧰 Tech Stack Summary

| | **HrFlow-Web** | **HrFlow-Desktop** |
|---|---|---|
| Language | PHP 8.0+ | Java 17 |
| Framework | Symfony 6.4 | JavaFX 21 |
| Build tool | Composer | Maven 3.8+ |
| UI | Twig + Tailwind CSS | FXML / JavaFX |
| Database | MySQL 8 (Doctrine ORM) | MySQL 8 (JDBC) |
| AI / external | Groq, Gemini, OpenRouter, NVIDIA, HuggingFace | Gemini, Groq (via AiServices) |
| Other | Docker, Symfony Messenger, Mailer | Multi-module Maven, JUnit 5 |

See the dedicated docs for full details:
- 👉 [HrFlow-Web/README.md](HrFlow-Web/README.md)
- 👉 [HrFlow-Desktop/README.md](HrFlow-Desktop/README.md)

---

## ✅ Prerequisites

Install the following before setting up the project:

- **Git**
- **PHP 8.0+** and **Composer** (for the web app)
- **Docker & Docker Compose** (optional but recommended for the web app)
- **Java JDK 17+** (for the desktop app)
- **Maven 3.8+** (for the desktop app)
- **MySQL 8** access (a remote managed instance is used by default)
- **Node.js 18+** (only if you rebuild front-end tooling outside of Symfony's asset pipeline)

---

## 🚀 Clone & Set Up the Full Project

```bash
# 1. Clone the repository
git clone <your-repo-url> HR-Flow-Full-APP
cd HR-Flow-Full-APP

# 2. Set up the WEB application
cd HrFlow-Web
cp .env.example .env          # then fill in your real values locally
composer install
# Option A — run with Docker:
docker-compose up -d
docker-compose exec php php bin/console doctrine:migrations:migrate
# Option B — run locally:
php bin/console doctrine:migrations:migrate
symfony serve                 # or: php -S localhost:8000 -t public

# 3. Set up the DESKTOP application
cd ../HrFlow-Desktop
cp .env.example .env          # then fill in your real values locally
mvn clean install             # builds every module
cd AppUi
mvn javafx:run                # or run.bat on Windows
```

Per-project setup details live in each sub-project README linked above.

---

## 🔐 Environment Variables

Both sub-projects are configured through `.env` files that are **never committed**.

- `HrFlow-Web/.env` — database URL, mailer, and AI/API keys (Symfony dotenv).
- `HrFlow-Desktop/.env` — database connection and AI keys consumed via environment
  variables / `config.properties`.

Each sub-project ships a committed `.env.example` listing every variable with a safe
placeholder value. Copy it and fill in your own secrets locally.

> ⚠️ **Security**: Never commit `.env` files. Copy `.env.example` to `.env` and fill in
> your values locally. The `.gitignore` files are configured to keep secrets out of the repo.

---

## 📄 License

This project is **proprietary**. Designed and developed by **Bechir Lahoueg**
([bechirlahoueg.tech](https://bechirlahoueg.tech)). All rights reserved.
Redistribution or commercial use is not permitted without explicit permission from the author.

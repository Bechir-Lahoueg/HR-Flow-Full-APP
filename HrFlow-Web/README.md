# HrFlow-Web

The **web** side of HR-Flow — a full Symfony 6.4 application that serves the HR platform
to browsers. It handles authentication, the HR/admin/employee dashboards, recruitment
flows, leave and training management, PDF/QR generation, e-mail notifications, and several
AI-powered features (emotion analysis, report generation, chat, churn prediction).

> ⚠️ **Security**: Never commit `.env` files. Copy `.env.example` to `.env` and fill in
> your values locally. All real secrets (database password, mailer password, API keys)
> must stay out of version control.

---

## 🧰 Tech Stack

- **PHP 8.0+**
- **Symfony 6.4** (framework-bundle, security, form, validator, serializer, messenger, mailer, notifier)
- **Doctrine ORM 3** + **Doctrine Migrations** (MySQL 8)
- **Twig** templating + **Symfony UX** (Turbo, Stimulus, Chart.js)
- **Tailwind CSS** via `symfonycasts/tailwind-bundle`
- **AssetMapper / Importmap** (no Webpack required)
- **dompdf** (PDF export) + **endroid/qr-code** (QR codes)
- **KnpPaginator** for pagination
- **Docker / Docker Compose** for local infra (PHP-FPM, Nginx, MySQL)
- **PHPUnit** + **PHPStan** for testing & static analysis
- External APIs: Groq, Google Gemini, OpenRouter, NVIDIA, HuggingFace, OpenWeather, NewsAPI, QuickChart, Matrix

---

## ⚙️ Installation

```bash
cd HrFlow-Web

# 1. Copy the environment template and fill in your secrets
cp .env.example .env

# 2. Install PHP dependencies
composer install
```

If you build the front-end assets, the Tailwind bundle compiles automatically through
Symfony; you can also trigger it manually:

```bash
php bin/console tailwind:build      # one-off build
php bin/console tailwind:build -w   # watch mode
```

---

## ▶️ Run in Dev Mode

**With the Symfony CLI (recommended):**

```bash
php bin/console doctrine:migrations:migrate   # apply DB schema
symfony serve                                  # https://localhost:8000
```

**With plain PHP:**

```bash
php -S localhost:8000 -t public
```

**With Docker:**

```bash
docker-compose up -d
docker-compose exec php composer install
docker-compose exec php php bin/console doctrine:migrations:migrate
```

The app will be available at `http://localhost:8000` (or the Symfony server URL).

---

## 🏗️ Build for Production

```bash
# Optimize the autoloader and install prod-only dependencies
composer install --no-dev --optimize-autoloader

# Set the environment to prod and warm the cache
APP_ENV=prod php bin/console cache:clear
APP_ENV=prod php bin/console cache:warmup

# Compile and dump assets
php bin/console tailwind:build --minify
php bin/console asset-map:compile
```

A `Dockerfile` and `render.yaml` are included for containerized / Render.com deployment.

---

## 🔑 Environment Variables

All variables are read from `.env` (and overridden by `.env.local` in real projects).
See [`.env.example`](.env.example) for placeholder values.

| Variable | Description | Secret? |
|---|---|:---:|
| `APP_ENV` | Symfony environment (`dev` / `prod` / `test`) | |
| `APP_SECRET` | Symfony app secret used for CSRF/signing | 🔑 **YES** |
| `DATABASE_URL` | Doctrine MySQL connection DSN (user, password, host, db) | 🔑 **YES** |
| `MESSENGER_TRANSPORT_DSN` | Symfony Messenger transport (Doctrine by default) | |
| `MAILER_DSN` | SMTP DSN for outgoing mail (contains mailbox password) | 🔑 **YES** |
| `MAILER_FROM` | Default "from" address for outgoing mail | |
| `MATRIX_CLIENT_URL` | Matrix homeserver URL for chat integration | |
| `MATRIX_ACCESS_TOKEN` | Matrix bot access token | 🔑 **YES** |
| `MATRIX_BOT_USER_ID` | Matrix bot user id | |
| `HUGGINGFACE_API_TOKEN` | HuggingFace inference API token (emotion model) | 🔑 **YES** |
| `HUGGINGFACE_EMOTION_MODEL` | HuggingFace model id for emotion detection | |
| `APP_PUBLIC_URL` | Public base URL of the app (used for absolute links/webhooks) | |
| `CORS_ALLOW_ORIGINS` | Comma-separated list of allowed CORS origins | |
| `UPLOAD_DIR` | Relative path where uploaded CVs are stored | |
| `OPENWEATHER_API_KEY` | OpenWeather API key | 🔑 **YES** |
| `NEWS_API_KEY` | NewsAPI key | 🔑 **YES** |
| `GEMINI_API_KEY` | Google Gemini API key | 🔑 **YES** |
| `OPEN_ROUTER_KEY` | OpenRouter API key | 🔑 **YES** |
| `RECRUITMENT_EMAILS_ENABLED` | Toggle automatic recruitment e-mails (`true`/`false`) | |
| `GROQ_API_KEY` | Groq API key (LLM inference) | 🔑 **YES** |
| `NVIDIA_API_KEY` | NVIDIA NIM API key | 🔑 **YES** |
| `GROQ_API` | Secondary Groq API key | 🔑 **YES** |
| `CHURN_API_URL` | URL of the local churn-prediction service | |
| `QUICKCHART_ENABLED` | Toggle QuickChart chart rendering (`true`/`false`) | |

> 🔑 Variables marked as secret contain credentials or API keys — keep them in `.env`
> (git-ignored) and never paste real values into `.env.example`, the README, or commits.

---

## 📂 Folder Structure

```
HrFlow-Web/
├── assets/             # Stimulus controllers & Tailwind source
├── bin/                # Symfony console (bin/console)
├── config/             # Bundles, routes, services, packages config
├── docker/             # Docker images / service configs
├── migrations/         # Doctrine migration classes
├── public/             # Web root — index.php + compiled assets
├── src/                # Application code
│   ├── Controller/     # HTTP controllers
│   ├── Entity/         # Doctrine entities
│   ├── Repository/     # Doctrine repositories
│   ├── Form/           # Symfony forms
│   └── Service/        # Business / integration services
├── templates/          # Twig templates
├── translations/       # i18n message catalogs
├── tests/              # PHPUnit tests
├── var/                # Cache & logs (git-ignored)
├── vendor/             # Composer dependencies (git-ignored)
├── composer.json       # PHP dependencies
├── Dockerfile          # Production container image
├── compose.yaml        # Docker Compose stack
├── render.yaml         # Render.com deployment config
└── .env.example        # Environment variable template
```

---

## 🚢 Deployment Notes

- **Render.com**: configured via `render.yaml`. Set every secret as an environment
  variable in the Render dashboard — do **not** ship a `.env` in the image.
- **Docker**: build with the provided `Dockerfile`; supply env vars at runtime
  (`docker run --env-file …` or orchestrator secrets), never bake them into the image.
- Always run with `APP_ENV=prod` and a fresh, warmed cache in production.
- Run `doctrine:migrations:migrate` on deploy to keep the schema up to date.
- Ensure the `UPLOAD_DIR` path is writable and persisted (volume) so uploaded CVs survive
  container restarts.

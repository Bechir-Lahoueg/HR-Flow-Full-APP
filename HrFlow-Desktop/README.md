# HrFlow-Desktop

The **desktop** side of HR-Flow — a **JavaFX 21** application built as a **multi-module
Maven** project. Each HR domain lives in its own Maven module, and the `AppUi` module is
the launchable entry point that depends on all of them and wires them into a single
JavaFX interface. The app talks directly to the shared MySQL database over JDBC and uses
the `AiServices` module for AI reporting, CSV import/export, and data visualization.

> ⚠️ **Security**: Never commit `.env` files or `config.properties` files that contain
> real credentials. Copy `.env.example` to `.env` and fill in your values locally.

---

## 🧰 Tech Stack

- **Java 17** (JDK)
- **JavaFX 21** (FXML-based UI)
- **Maven 3.8+** (multi-module build)
- **MySQL 8** via JDBC (`mysql-connector-j`)
- **JUnit 5** for tests
- AI integrations in `AiServices` (Google Gemini, Groq) for report generation & data analysis

### Modules

| Module | Responsibility |
|---|---|
| `AppUi` | JavaFX entry point — aggregates all modules and launches the UI |
| `AiServices` | AI reporting, data visualization, CSV import/export |
| `GestionUtilisateur` | User & authentication management |
| `GestionDesConges` | Leave requests & approvals |
| `GestionRecrutement` | Recruitment & candidates |
| `Gestionformation` | Training management |
| `GestionRelationEmployees` | Employee relations |
| `GestionRemuneration` | Compensation / payroll |

---

## ▶️ Build & Run

From the `HrFlow-Desktop` directory:

```bash
# Copy and fill the environment template
cp .env.example .env

# Build every module and install them into the local Maven repo
mvn clean install

# Run the JavaFX application (AppUi is the entry point)
cd AppUi
mvn javafx:run
```

On **Windows**, you can also use the provided launcher:

```bat
cd AppUi
run.bat
```

To build a single module without the whole reactor:

```bash
mvn -pl GestionDesConges clean compile
mvn -pl GestionDesConges test
```

---

## 🔑 Config & Environment Variables

The desktop app reads its configuration from **environment variables** first, then falls
back to a `config.properties` file (used by the `AiServices` reporting agents). When an
environment variable is not set, modules fall back to sensible/coded defaults — override
them locally via `.env` or your shell.

See [`.env.example`](.env.example) for placeholder values.

| Variable | Description | Secret? |
|---|---|:---:|
| `DB_URL` | JDBC URL of the MySQL database | |
| `DB_USER` | Database username | 🔑 **YES** |
| `DB_PASSWORD` | Database password | 🔑 **YES** |
| `GEMINI_API_KEY` | Google Gemini API key (AI report generation) | 🔑 **YES** |
| `GROQ_API_KEY` | Groq API key (LLM inference fallback) | 🔑 **YES** |
| `LLM_PROVIDER` | Which LLM provider the AI agents use (e.g. `gemini` / `groq`) | |

`AiServices` may also load these from a `config.properties` file on the classpath
(keys: `db.url`, `db.user`, `db.password`, `gemini.api.key`, `groq.api.key`,
`llm.provider`). **That file must not be committed** — it is git-ignored.

---

## 📂 Folder Structure

```
HrFlow-Desktop/
├── AppUi/                       # JavaFX entry point (launchable)
│   ├── src/main/java/           # UI controllers (org.example.ui…)
│   ├── src/main/resources/fxml/ # FXML views
│   ├── run.bat                  # Windows launcher
│   └── pom.xml
├── AiServices/                  # AI reporting / CSV / data viz
│   └── src/main/resources/config.properties  # local config (git-ignored)
├── GestionUtilisateur/          # User management module
├── GestionDesConges/            # Leave management module
├── GestionRecrutement/          # Recruitment module
├── Gestionformation/            # Training module
├── GestionRelationEmployees/    # Employee relations module
├── GestionRemuneration/         # Compensation module
└── .env.example                 # Environment variable template
```

Each business module follows a simple MVC layout:

```
src/main/java/org/example/
├── config/      # Database configuration
├── controller/  # Business controllers
├── model/       # Data models
├── service/     # Business logic + JDBC access
└── Main.java    # Module entry point (where applicable)
```

---

## 📦 Export / Package the App

Package the application (and its dependencies) into runnable artifacts with Maven:

```bash
# Build a fat/shaded JAR for the UI module
mvn clean package -pl AppUi -am
```

The shaded JAR is produced under `AppUi/target/`. Because JavaFX requires its native
runtime modules, prefer one of the following for distribution:

```bash
# Run from the shaded jar (JavaFX modules on the module path)
java -jar AppUi/target/AppUi-1.0-SNAPSHOT.jar

# Or produce a self-contained native installer with jpackage (JDK 17+)
jpackage \
  --name HrFlow \
  --input AppUi/target \
  --main-jar AppUi-1.0-SNAPSHOT.jar \
  --type app-image
```

> Built JARs and the `target/` directory are git-ignored — only source and `pom.xml`
> files are committed.

# Code Noir

**Developer Onboarding Intelligence for Unfamiliar Codebases**

Code Noir is an IBM Bob-powered developer onboarding assistant that analyzes unfamiliar repositories and converts them into a structured onboarding experience.

Instead of manually exploring folders, configuration files, architecture, and important source files, developers can upload a repository and receive a guided report showing what the project does, how it is structured, what files matter most, what to read first, and where to begin contributing.

---

## Problem

Developers often spend significant time understanding an unfamiliar codebase before they can contribute effectively.

This is especially common in:

- New team onboarding
- Internships
- Hackathons
- Open-source contributions
- Legacy project handovers
- Rapidly evolving development teams

Documentation may be incomplete, outdated, or spread across the repository.

---

## Solution

Code Noir transforms an uploaded repository into a structured developer onboarding report.

The system identifies:

- Project purpose
- Primary programming language
- Frameworks
- Build tool
- Architecture
- Important folders
- Key files and their responsibilities
- Repository observations
- Recommended reading order
- Suggested starter task
- Stack-specific technical details

The output is presented through a clean, developer-focused dashboard instead of a large unstructured AI response.

---

## How IBM Bob Is Used

IBM Bob is the core repository intelligence engine behind Code Noir.

When a user uploads a ZIP repository:

1. The Spring Boot backend safely extracts the project.
2. The application invokes IBM Bob through **Bob Shell** using Java `ProcessBuilder`.
3. Bob analyzes the repository structure and code.
4. Bob generates a structured `codenoir-report.json`.
5. The Java application parses the report using Jackson.
6. Thymeleaf renders the structured onboarding dashboard.

IBM Bob is therefore not used only during development — it is directly integrated into the runtime workflow of Code Noir.

---

## Architecture

```text
ZIP Repository
      ↓
Spring Boot Upload & Extraction
      ↓
IBM Bob Shell Analysis
      ↓
codenoir-report.json
      ↓
Jackson / OnboardingReport
      ↓
Thymeleaf Dashboard
```

---

## Language-Agnostic Analysis

Code Noir is designed around a standardized report structure rather than hardcoded rules for a specific programming language or framework.

IBM Bob analyzes the uploaded repository and maps its findings into the same onboarding structure regardless of the underlying technology stack.

The workflow has been tested with repositories using:

- Java / Spring Boot
- Python / Flask

This allows the dashboard to provide a consistent onboarding experience across different types of projects.

---

## Key Features

- Upload and analyze repositories as ZIP files
- IBM Bob-powered repository analysis
- Automatic technology stack identification
- Architecture and project-purpose discovery
- Important folder identification
- Key file explanations
- Repository observations and warnings
- Recommended code-reading order
- Suggested starter task for new contributors
- Structured, interactive onboarding dashboard
- Language-agnostic analysis workflow
- Safe ZIP extraction
- No database or persistent storage required

---

## Tech Stack

### Backend

- Java
- Spring Boot
- Maven
- Jackson

### Frontend

- Thymeleaf
- HTML
- CSS
- JavaScript

### Repository Intelligence

- IBM Bob
- Bob Shell

---

## Running Code Noir Locally

### Prerequisites

Before running Code Noir, make sure you have:

- Java 17 or later
- Maven, or use the included Maven Wrapper
- IBM Bob / Bob Shell installed and configured
- A valid IBM Bob API key

---

### 1. Clone the Repository

```bash
git clone https://github.com/TechFatima/CODE-NOIR.git
cd CODE-NOIR
```

---

### 2. Configure IBM Bob

Code Noir invokes IBM Bob at runtime through Bob Shell.

Set your IBM Bob API key as an environment variable before starting the application.

#### Windows PowerShell

```powershell
$env:BOB_API_KEY="your_api_key_here"
```

To verify that the environment variable is available:

```powershell
$env:BOB_API_KEY
```

> **Important:** Use your own valid IBM Bob API key. Never commit API keys, credentials, or other secrets to the repository.

---

### 3. Run the Application

Using the included Maven Wrapper on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Or, if Maven is installed globally:

```bash
mvn spring-boot:run
```

---

### 4. Open Code Noir

Once the application starts, open:

```text
http://localhost:8080
```

Upload a repository as a ZIP file and select **Analyze Repository**.

Code Noir will extract the repository, invoke IBM Bob for analysis, process the generated report, and display the results through the onboarding dashboard.

Repository analysis may take a short amount of time depending on the project.

---

## Analysis Output

IBM Bob generates a structured report that Code Noir maps into the following main sections:

```text
Project
├── Name
├── Summary
├── Primary Language
├── Frameworks
├── Build Tool
└── Architecture

Important Folders
Important Files
Repository Observations
Recommended Reading Order
Starter Task
Technical Details
```

This standardized structure allows Code Noir to present different repositories through the same onboarding interface.

---

## Security

Code Noir avoids hardcoding IBM Bob credentials.

The Bob API key is supplied through the:

```text
BOB_API_KEY
```

environment variable.

The application also performs protected ZIP extraction to prevent unsafe archive paths from being written outside the intended extraction directory.

API keys and other credentials should never be committed to source control.

---

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── ...        # Spring Boot backend
│   └── resources/
│       ├── static/     # CSS and frontend assets
│       └── templates/  # Thymeleaf templates
├── test/
│   └── ...             # Tests
pom.xml
mvnw
mvnw.cmd
```

---

## Future Scope

Potential improvements include:

- Support for additional repository input methods
- GitHub repository URL analysis
- More detailed dependency visualization
- Interactive architecture exploration
- Improved onboarding recommendations
- Repository comparison and change analysis
- Team-specific onboarding profiles
- Deployment as a hosted developer tool

---

## Built For

**IBM Bob 2.0 Hackathon**

Code Noir was developed as a solo hackathon project exploring how IBM Bob can be integrated directly into a developer tool as a runtime repository intelligence engine.

---

## Repository

GitHub: https://github.com/TechFatima/CODE-NOIR

---

**Code Noir — turning unfamiliar codebases into a clear place to begin.**

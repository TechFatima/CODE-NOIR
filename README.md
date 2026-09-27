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

The output is presented through a clean developer-focused dashboard instead of a large unstructured AI response.

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

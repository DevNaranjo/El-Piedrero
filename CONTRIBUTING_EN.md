# Contributing Guide for El Piedrero 🤝

[🇪🇸 Español](CONTRIBUTING.md) • [🇬🇧 English](CONTRIBUTING_EN.md)

Thank you for your interest in contributing to **El Piedrero**! This is an open-source project created to preserve, celebrate, and modernize score tracking for the traditional **Canary Ronda** card game.

---

## 🛠️ Development Requirements

1. **Android Studio:** Iguana | 2023.2.1 or newer.
2. **JDK:** Java 17 (automatically managed via Gradle Toolchains).
3. **Testing Device:** Android 7.0 (API 24) or newer (physical phone or emulator with camera and Wi-Fi capabilities).

---

## 🚀 Contribution Workflow

1. **Fork** this repository on GitHub.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/El-Piedrero.git
   cd El-Piedrero
   ```
3. Create a feature branch with a descriptive name:
   ```bash
   git checkout -b feature/your-feature-name
   # or
   git checkout -b fix/issue-description
   ```
4. Implement your changes following:
   * Official Kotlin and Jetpack Compose coding conventions.
   * Verify that all unit tests pass:
     ```bash
     ./gradlew test
     ```
5. Commit your work using clean Conventional Commit messages with emojis:
   ```bash
   git commit -m "feat: add support for custom table sounds"
   ```
6. Push your branch to your fork and submit a **Pull Request** to the `main` branch, explaining your changes and reasoning.

---

## 📜 License

By contributing to **El Piedrero**, you agree that your contributions will be licensed under the terms of the [MIT License](LICENSE).

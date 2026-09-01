# Guía de Contribución para El Piedrero 🤝

¡Gracias por tu interés en colaborar con **El Piedrero**! Este es un proyecto de código abierto desarrollado para preservar y modernizar el conteo de la tradicional **Ronda Canaria**.

---

## 🛠️ Requisitos de Desarrollo

1. **Android Studio:** Iguana | 2023.2.1 o superior.
2. **JDK:** Java 17 (resuelto automáticamente mediante Gradle Toolchains).
3. **Dispositivo de prueba:** Android 7.0 (API 24) o superior (físico o emulador con cámara y Wi-Fi).

---

## 🚀 Flujo de Trabajo para Contribuir

1. Haz un **Fork** de este repositorio en GitHub.
2. Clona tu fork localmente:
   ```bash
   git clone https://github.com/TU_USUARIO/ElPiedrero.git
   cd ElPiedrero
   ```
3. Crea una rama descriptiva para tu cambio:
   ```bash
   git checkout -b feature/nueva-funcionalidad
   # o
   git checkout -b fix/correccion-bug
   ```
4. Realiza tus cambios asegurándote de:
   * Mantener el estilo oficial de Kotlin y Jetpack Compose.
   * Ejecutar y verificar que los tests unitarios pasen:
     ```bash
     ./gradlew test
     ```
5. Haz commit con mensajes claros y descriptivos:
   ```bash
   git commit -m "feat: añadir nueva funcionalidad X"
   ```
6. Envía un **Pull Request** a la rama `main` de este repositorio explicando tu propuesta.

---

## 📜 Licencia

Al contribuir a **El Piedrero**, aceptas que tus contribuciones se publicarán bajo los términos de la [Licencia MIT](LICENSE).

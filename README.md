# Simple Task Management

Aplicación Android para organizar tareas con Jetpack Compose. Guarda los datos localmente y permite sincronizar tareas con Google Tasks.

## Funcionalidades

- Crear, editar, completar y eliminar tareas.
- Organizar tareas en categorías y buscar en la lista.
- Añadir descripción, fecha, hora y repetición a una tarea.
- Dictar el título de una tarea mediante el reconocimiento de voz del dispositivo.
- Usar la aplicación sin conexión y sincronizar con Google Tasks cuando haya conexión.
- Iniciar sesión y autorizar la sincronización con una cuenta de Google.
- Cambiar entre tema claro y oscuro.

## Tecnologías

- Kotlin y Jetpack Compose con Material 3.
- Arquitectura por capas: presentación (MVVM), dominio y datos.
- Room para almacenamiento local.
- Retrofit y OkHttp para la API de Google Tasks.
- Hilt para inyección de dependencias.
- WorkManager para sincronizaciones en segundo plano.
- Credential Manager y Google Identity Services para autorización.

## Requisitos

- Android Studio compatible con Android Gradle Plugin 9.2.1.
- JDK 21; Gradle usa la configuración de toolchain incluida en el proyecto.
- Android SDK Platform 36 (API 36.1).

Abre el proyecto en Android Studio y espera a que termine la sincronización de Gradle. También puedes compilar desde la terminal:

```powershell
.\gradlew.bat assembleDebug
```

Para ejecutar las pruebas unitarias:

```powershell
.\gradlew.bat testDebugUnitTest
```

En macOS o Linux, usa `./gradlew` en lugar de `.\gradlew.bat`.

## Configurar Google Tasks

La sincronización requiere configurar un proyecto en [Google Cloud Console](https://console.cloud.google.com/):

1. Habilita la API de Google Tasks.
2. Configura la pantalla de consentimiento OAuth y crea un cliente OAuth para Android con el identificador de aplicación `com.example.simpletaskmanagement` y la huella SHA-1 del certificado con el que compilarás.
3. Autoriza la aplicación con una cuenta de Google desde Ajustes para conceder el permiso de Google Tasks.

La aplicación solicita el alcance `https://www.googleapis.com/auth/tasks`. No guardes credenciales privadas ni tokens en el repositorio. El identificador de AdMob del manifiesto es el identificador de prueba de Google; reemplázalo por uno propio antes de distribuir la aplicación.

## Estructura del proyecto

```text
app/src/main/java/com/example/simpletaskmanagement/
├── data/          # Room, API remota, autenticación, repositorio y sincronización
├── di/            # Módulos de Hilt
├── domain/        # Modelos, contratos de repositorio y casos de uso
├── presentation/  # Pantallas Compose, navegación y ViewModels
└── ui/theme/      # Tema y estilos
```

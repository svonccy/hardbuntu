# 🤝 Guía de Colaboración y Configuración del Entorno

Bienvenido a **Hardbuntu**. Esta guía detalla cómo configurar tu entorno local de desarrollo, las herramientas de IA (Antigravity & MCPs), la conexión con Supabase y las directrices de contribución con Git.

> 📖 **Cheat Sheet:** Para una lista rápida de comandos de terminal frecuentes (Android, Supabase y Git), consulta **[Comandos Útiles](COMANDOS_UTILES.md)**.

---

## 📌 Índice
1. [El archivo `local.properties` (El `.env` de Android)](#1-el-archivo-localproperties-el-env-de-android)
2. [Configuración de MCPs para Asistencia con IA (Antigravity)](#2-configuración-de-mcps-para-asistencia-con-ia-antigravity)
3. [Instalación de Agent Skills de Supabase](#3-instalación-de-agent-skills-de-supabase)
4. [Convención de Commits en Git (Español e Infinitivo)](#4-convención-de-commits-en-git-español-e-infinitivo)
5. [Buenas Prácticas de Arquitectura](#5-buenas-prácticas-de-arquitectura)

---

## 1. El archivo `local.properties` (El `.env` de Android)

En el desarrollo web tradicional se utiliza comúnmente un archivo `.env` para almacenar variables de entorno y secretos locales. En **Android / Gradle**, el estándar nativo para este propósito es el archivo **`local.properties`**.

* **Nunca se versiona en Git:** Está explícitamente ignorado en `.gitignore`.
* **Es leído por Gradle:** Permite inyectar claves de API en tiempo de compilación a través de `BuildConfig` sin dejarlas grabadas en código fuente público.

### Pasos para configurarlo:
1. En la raíz del proyecto, copia el archivo de plantilla:
   ```bash
   cp local.properties.example local.properties
   ```
2. Abre `local.properties` y verifica que contenga tus rutas y credenciales:
   ```properties
   # Ruta a tu SDK local de Android
   sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk

   # Credenciales de Supabase
   SUPABASE_URL=https://pwvttubqvhcyzgwaymdn.supabase.co
   SUPABASE_ANON_KEY=sb_publishable_B-eqzaLvDgCgAKKUJCL7ag_pPCg3Vxi
   ```

---

## 2. Configuración de MCPs para Asistencia con IA (Antigravity)

Si desarrollas utilizando **Google Antigravity**, el proyecto cuenta con soporte para servidores de **Model Context Protocol (MCP)** para que la IA pueda consultar la base de datos de Supabase y la documentación oficial en tiempo real.

### Pasos para configurar tus MCPs locales:
1. Copia el archivo base de configuración en la carpeta `.agents/`:
   ```bash
   cp .agents/mcp_config.example.json .agents/mcp_config.json
   ```
   > ℹ️ **Nota de Seguridad:** El archivo `.agents/mcp_config.json` está ignorado en `.gitignore` para no exponer credenciales personales a GitHub.

2. **Supabase MCP:**
   El archivo de ejemplo ya apunta al proyecto compartido:
   `https://mcp.supabase.com/mcp?project_ref=pwvttubqvhcyzgwaymdn...`
   * Al iniciar o reiniciar Antigravity, se te solicitará autorizar la conexión mediante **OAuth 2.1**. Completa el flujo en el navegador con tu cuenta de Supabase que tenga acceso al proyecto.
   * Si requieres autenticar manualmente: abre la configuración de Antigravity (`Ctrl+,` o `Cmd+,`), ve a la pestaña **Customizations** y haz clic en **Authenticate** junto al servidor Supabase.

3. **Context7 MCP (Opcional):**
   * Si cuentas con una API key personal de Context7, colócala en el campo `Authorization: Bearer <TU_TOKEN>` en `.agents/mcp_config.json`.

---

## 3. Instalación de Agent Skills de Supabase

Las **Agent Skills** permiten al asistente de IA comprender las mejores prácticas oficiales de Supabase (modelado Postgres, políticas RLS, optimización de consultas).

Para actualizar o añadir las skills oficiales en tu entorno local:
```bash
npx skills add supabase/agent-skills
```
Esto mantendrá sincronizadas las directrices en `.agents/skills/`.

---

## 4. Convención de Commits en Git (Español e Infinitivo)

Para mantener un historial limpio, trazable y profesional, todos los colaboradores deben seguir el estándar de **Conventional Commits**, pero adaptado con las siguientes dos reglas estrictas del proyecto:

1. **Idioma:** Español.
2. **Tiempo verbal:** **Infinitivo** (terminaciones *-ar*, *-er*, *-ir*), dado que el modo imperativo presenta ambigüedades en español.

### Estructura:
```text
<tipo>(<alcance_opcional>): <verbo en infinitivo> <descripción del cambio>
```

### Tipos permitidos:
| Tipo | Cuándo usarlo | Ejemplo |
| :--- | :--- | :--- |
| `feat` | Nueva funcionalidad o pantalla | `feat: agregar pantalla de detalle de producto con ficha técnica` |
| `fix` | Corrección de un bug o error | `fix: corregir cálculo del impuesto IGV en el carrito` |
| `docs` | Cambios exclusivamente en documentación | `docs: documentar proceso de onboarding para colaboradores` |
| `style` | Formato, espacios, punto y coma (sin cambios de lógica) | `style: formatear composables según guía oficial` |
| `refactor` | Refactorización de código sin alterar comportamiento | `refactor: extraer lógica de cálculo de precios a un caso de uso` |
| `test` | Añadir o corregir pruebas unitarias o de UI | `test: agregar pruebas unitarias para CartViewModel` |
| `chore` | Tareas de mantenimiento, dependencias o Gradle | `chore: actualizar versión de supabase-kt a v3.0` |

---

## 5. Buenas Prácticas de Arquitectura

* **No usar código Quickstart en componentes de UI:** El código genérico de inicio rápido de Supabase (por ejemplo, llamadas a `supabase.from(...)` dentro de un `LaunchedEffect` en la `Activity`) está prohibido en este repositorio.
* **Separación de Capas (MVVM):**
  * La UI (`@Composable`) solo interactúa con el `ViewModel` mediante eventos y consume `StateFlow`.
  * El `ViewModel` solicita datos al `Repository`.
  * El `Repository` gestiona la llamada al cliente de Supabase (`postgrest-kt`, `auth-kt`).
* **Seguridad de Claves:** Jamás declarar credenciales *hardcodeadas* en clases `.kt`. Se deben leer siempre a través de `BuildConfig` generado desde `local.properties`.

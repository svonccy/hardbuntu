# ⚡ Comandos Útiles y Cheat Sheet de Desarrollo

Referencia rápida de comandos frecuentes para el desarrollo diario en **Hardbuntu** (Android / Gradle, Supabase y Git).

---

## 🤖 1. Android & Gradle Wrapper (`gradlew`)

> 💡 **En Windows:** Usa `.\gradlew.bat`. **En macOS/Linux:** Usa `./gradlew`.

### Compilación y Construcción
```bash
# Compilar únicamente las clases de Kotlin (verificación ultrarrápida de sintaxis y tipos)
.\gradlew.bat compileDebugKotlin

# Generar el archivo APK de depuración (app-debug.apk en app/build/outputs/apk/debug/)
.\gradlew.bat assembleDebug

# Instalar y ejecutar la app en tu teléfono o emulador conectado
.\gradlew.bat installDebug

# Limpiar archivos temporales y cachés de compilación
.\gradlew.bat clean
```

### Pruebas y Diagnóstico
```bash
# Ejecutar todas las pruebas unitarias locales (JVM)
.\gradlew.bat test

# Inspeccionar el árbol completo de dependencias de la app
.\gradlew.bat app:dependencies

# Listar todas las tareas disponibles
.\gradlew.bat tasks
```

---

## 🗄️ 2. Supabase CLI (Gestión de Base de Datos y Migraciones)

> 💡 Puedes ejecutar estos comandos con `npx supabase <comando>` sin necesidad de instalación global previa.

### Entorno Local (Docker)
```bash
# Iniciar Postgres, Auth, Storage y Studio en tu computadora
npx supabase start

# Detener el stack local para liberar memoria y puertos
npx supabase stop

# Reiniciar la base de datos local desde cero (reaplica migraciones y seed.sql)
npx supabase db reset
```

### Migraciones y Esquema
```bash
# Crear un nuevo archivo de migración vacío con marca de tiempo
npx supabase migration new nombre_de_la_migracion

# Ver diferencias entre tu base de datos local y las migraciones escritas
npx supabase db diff

# Aplicar las migraciones locales a tu proyecto remoto en la nube (con precaución)
npx supabase db push

# Vincular este directorio con tu proyecto remoto de Supabase
npx supabase link --project-ref pwvttubqvhcyzgwaymdn
```

---

## 🐙 3. Git & GitHub CLI (`gh`)

### Flujo de Trabajo y Commits Convencionales
> ⚠️ **Regla del Proyecto:** Commits en **español** y con verbos en **infinitivo** (*agregar*, *corregir*, *actualizar*, *configurar*).

```bash
# Ver estado actual del árbol de trabajo
git status

# Añadir cambios al área de preparación (stage)
git add .

# Ejemplos de commits convencionales válidos:
git commit -m "feat: implementar cliente de supabase en capa remota"
git commit -m "fix: corregir calculo de subtotal sin IGV en el carrito"
git commit -m "docs: actualizar instrucciones en comandos utiles"
git commit -m "chore: actualizar libreria coil a version reciente"

# Enviar commits a GitHub
git push

# Ver los últimos 5 commits de forma compacta
git log --oneline -n 5
```

### GitHub CLI (`gh`)
```bash
# Ver información del repositorio remoto
gh repo view

# Abrir el repositorio directamente en tu navegador web predeterminado
gh repo view --web

# Ver estado de los Pull Requests abiertos
gh pr list
```

---

## 🧠 4. Herramientas de IA & Agent Skills

```bash
# Instalar o actualizar las Agent Skills de Supabase para Antigravity
npx skills add supabase/agent-skills
```

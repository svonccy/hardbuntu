# 🛠️ Hardbuntu

Aplicación móvil nativa para Android de comercio electrónico (**E-commerce**) especializada en el sector ferretero, venta de herramientas eléctricas y manuales, insumos de construcción y seguridad industrial.

El proyecto está desarrollado en **Kotlin** siguiendo la [Guía Oficial de Arquitectura de Android (developer.android.com)](https://developer.android.com/topic/architecture), utilizando **Jetpack Compose**, **Material 3** y **Supabase** como Backend-as-a-Service (BaaS).

---

## 📋 Características Principales

* **Catálogo Técnico Especializado:** Exploración y filtrado reactivo de productos por categorías (Herramientas Eléctricas, Manuales, Tornillería y Fijaciones, Gasfitería, Seguridad Industrial).
* **Fichas Técnicas Dinámicas:** Especificaciones técnicas completas (voltaje, potencia, RPM, calibre, unidad de medida) modeladas en PostgreSQL/JSONB.
* **Carrito de Compras y Cotización:** Cálculo de subtotales, desglose de impuestos peruano (**IGV 18%**) y control de stock en tiempo real.
* **Checkout y Gestión de Pedidos:** Registro y seguimiento de órdenes sincronizadas en la nube mediante Supabase PostgREST y RLS.
* **UI Industrial Adaptativa:** Componentes visuales robustos con Material Design 3, soporte para modo oscuro/claro y previsualizaciones dinámicas (`@Preview`).

---

## 🏛️ Arquitectura de la Aplicación

La arquitectura sigue las directrices oficiales de **Google Android Jetpack Architecture (UDF - Unidirectional Data Flow)** y Clean Architecture simplificada:

```
UI Layer (Jetpack Compose + Material 3)
      │  ▲
Event │  │ State (StateFlow / collectAsStateWithLifecycle)
      ▼  │
ViewModel (State Holders + Coroutines viewModelScope)
      │  ▲
      ▼  │
Data Layer (Repositories)
      │
      ├── Remote Data Source (Supabase Client: PostgREST, Auth, Storage)
      └── Local / Cache (SavedStateHandle, StateFlow)
```

### 📂 Estructura de Paquetes (`com.hardbuntu.app`)

```text
com.hardbuntu.app
├── data
│   ├── model             # Entidades y DTOs serializables (@Serializable)
│   ├── remote            # Cliente Supabase y proveedores de red Ktor
│   └── repository        # Implementación de repositorios (ProductRepository, CartRepository)
├── domain
│   └── model             # Modelos de dominio y reglas de negocio (cálculo IGV, validaciones)
├── ui
│   ├── components        # Componentes reutilizables (ProductCard, PriceTag, IndustrialTopBar)
│   ├── navigation        # Rutas tipadas (@Serializable) y NavHost con Type-Safe Navigation
│   ├── screens
│   │   ├── catalog       # Catálogo y búsqueda de productos (CatalogScreen, CatalogViewModel)
│   │   ├── detail        # Ficha técnica y detalle (ProductDetailScreen, ProductDetailViewModel)
│   │   ├── cart          # Carrito y desglose de cotización (CartScreen, CartViewModel)
│   │   └── checkout      # Resumen de compra y confirmación (CheckoutScreen, CheckoutViewModel)
│   └── theme             # Sistema de diseño (Color.kt, Theme.kt, Type.kt con acentos industriales)
└── MainActivity.kt       # Activity principal y punto de entrada Compose
```

---

## 🚀 Stack Tecnológico y Librerías Oficiales

### 1. Frontend Android (Nativo)
* **Lenguaje:** [Kotlin 2.2+](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/develop/ui/compose) + [Material Design 3](https://developer.android.com/develop/ui/compose/designsystems/material3)
* **Navegación:** **Navigation Compose 2.8+ con Type-Safe Navigation** (`@Serializable` routes, eliminando rutas por strings).
* **Gestión de Estado y Ciclo de Vida:**
  * `androidx.lifecycle:lifecycle-viewmodel-compose`
  * `androidx.lifecycle:lifecycle-runtime-compose` (`collectAsStateWithLifecycle` para consumo eficiente de recursos en UI).
* **Serialización:** [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) (`kotlinx-serialization-json`).
* **Carga de Imágenes:** [Coil 3](https://coil-kt.github.io/coil/) (`io.coil-kt.coil3:coil-compose` con motor de red Ktor).

### 2. Backend & Base de Datos (Supabase BaaS)
* **Plataforma:** [Supabase](https://supabase.com/) (PostgreSQL 15+, Auth, Storage y PostgREST).
* **SDK Cliente:** [supabase-kt v3](https://github.com/supabase-community/supabase-kt)
  * `postgrest-kt`: Consultas estructuradas al catálogo y pedidos.
  * `auth-kt`: Autenticación de usuarios y clientes.
  * `storage-kt`: Descarga de imágenes de productos y fichas técnicas.
* **Cliente HTTP:** [Ktor Client](https://ktor.io/) (`ktor-client-android`).

### 3. Asistencia de Desarrollo por IA y MCPs
* **Context7 MCP (`@upstash/context7-mcp`):** Consulta directa de la documentación oficial de Google Android y librerías en tiempo real.
* **Supabase MCP:** Gestión de esquemas, migraciones y políticas RLS (Row Level Security).

---

## ⚙️ Configuración y Requisitos

### Requisitos Previos
* **Android Studio:** Ladybug (2024.2+) o superior con JDK 17 o 21.
* **Android SDK:**
  * `minSdk`: 26 (Android 8.0 Oreo)
  * `targetSdk` / `compileSdk`: 37 (Android 15+)
* Cuenta de **Supabase** activa con un proyecto configurado.

### Variables de Configuración Segura
Para conectar con Supabase sin exponer credenciales en el repositorio de Git, configurar en `local.properties`:

```properties
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key-aqui
```

---

## 🛠️ Comandos de Compilación

Compilar la aplicación en modo Debug desde la consola:

```bash
# En Windows (PowerShell / CMD)
.\gradlew.bat assembleDebug

# Comprobar tareas y dependencias
.\gradlew.bat tasks
```

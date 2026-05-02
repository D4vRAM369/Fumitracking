# PBL: Limpieza de Warnings de Gradle / Android Gradle Plugin

**Fecha:** 2026-05-02  
**Proyecto:** CBDCounter2  
**Tipo de sesión:** mantenimiento de build  
**Objetivo:** entender, corregir y documentar los warnings que aparecían en Android Studio al ejecutar Gradle.

---

## Resumen Corto

Los mensajes amarillos no eran errores de la app. Eran avisos del Android Gradle Plugin indicando que algunas propiedades en `gradle.properties` fuerzan comportamientos antiguos. La build seguía funcionando, pero esas opciones están marcadas para desaparecer en versiones futuras.

La solución fue eliminar las propiedades legacy que ya no hacían falta y activar la propiedad recomendada para mejorar el manejo de constraints de dependencias.

Archivo corregido:

```properties
gradle.properties
```

Verificación:

```bash
bash ./gradlew clean :app:compileDebugKotlin
```

Resultado: build correcta con la mayoría de warnings corregidos. Queda una advertencia de compatibilidad (`android.builtInKotlin=false`) porque retirarla rompe la integración actual entre AGP 9.1.1, Kotlin explícito y los unit tests. `android.newDsl=false` también sigue siendo necesario para que esa combinación compile, aunque en la última verificación no fue el warning visible.

---

## 1. `android.usesSdkInManifest.disallowed=false`

### Descripción

Esta propiedad permitía seguir aceptando configuración de SDK desde el `AndroidManifest.xml`.

### Causa

El proyecto tenía una bandera antigua que desactiva la validación moderna del Android Gradle Plugin.

### Cómo Afecta

No rompía la app hoy, pero deja el proyecto atado a un comportamiento legacy. En una versión futura del plugin, esa propiedad será eliminada y podría convertirse en fallo de configuración.

### Solución

Se eliminó la propiedad. El proyecto ya declara `minSdk` y `targetSdk` correctamente en `app/build.gradle.kts`, y el manifest no contiene `<uses-sdk>`.

---

## 2. `android.sdk.defaultTargetSdkToCompileSdkIfUnset=false`

### Descripción

Controlaba qué hace Gradle si un módulo no define `targetSdk`.

### Causa

Era una bandera de compatibilidad para proyectos incompletos o antiguos.

### Cómo Afecta

En este proyecto no aportaba nada porque `targetSdk = 35` está definido explícitamente. Mantenerla solo generaba ruido y riesgo futuro.

### Solución

Se eliminó la propiedad. La configuración explícita en `defaultConfig` sigue siendo la fuente de verdad.

---

## 3. `android.enableAppCompileTimeRClass=false`

### Descripción

Forzaba un comportamiento antiguo de generación de la clase `R` usada para acceder a recursos.

### Causa

Probablemente se añadió para esquivar algún problema anterior de compilación o migración.

### Cómo Afecta

Puede hacer la compilación menos alineada con el comportamiento actual de AGP. No cambia la UI por sí misma, pero mantiene una ruta de build antigua.

### Solución

Se eliminó la propiedad y se dejó el default moderno del plugin.

---

## 4. `android.builtInKotlin=false`

### Descripción

Desactivaba la integración Kotlin incorporada del Android Gradle Plugin.

### Causa

Es otra bandera legacy. En AGP 9, Android trae integración Kotlin incorporada. El proyecto también aplicaba el plugin Kotlin explícito:

```kotlin
id("org.jetbrains.kotlin.android")
```

### Cómo Afecta

Mientras `android.builtInKotlin=false` estaba presente, el proyecto compilaba por la ruta antigua. Al quitar esa bandera sin tocar el plugin explícito apareció el error real:

```text
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

Esto significa que AGP y el plugin Kotlin intentaban registrar la misma extensión.

### Solución

Se intentó eliminar la propiedad y quitar `id("org.jetbrains.kotlin.android")` de `app/build.gradle.kts` para usar Kotlin integrado en AGP. La app compilaba, pero los unit tests dejaron de ver clases Kotlin de `main` (`BackupManager`, `CsvCodec`, `HeatmapUtils`, etc.).

También se probó quitar solo `android.builtInKotlin=false`, pero apareció:

```text
Cannot add extension with name 'kotlin', as there is an extension already registered with that name.
```

La solución segura actual fue conservar `android.builtInKotlin=false` junto con el plugin Kotlin explícito. Es deuda técnica, pero evita romper tests.

---

## 5. `android.newDsl=false`

### Descripción

Forzaba el DSL antiguo de Android Gradle Plugin.

### Causa

La propiedad se quedó en `gradle.properties` como compatibilidad con versiones anteriores.

### Cómo Afecta

Puede retrasar migraciones y generar avisos en cada build. El proyecto ya usa `build.gradle.kts`, así que conviene dejar que AGP use su DSL actual.

### Solución

Se intentó eliminar la propiedad, pero el plugin Kotlin explícito falló contra el DSL nuevo:

```text
ApplicationExtensionImpl cannot be cast to BaseExtension
```

La solución segura actual fue conservar `android.newDsl=false`. En la última verificación no fue el warning visible, pero retirarlo rompe la build con la combinación actual de AGP/Kotlin.

---

## 6. `android.r8.optimizedResourceShrinking=false`

### Descripción

Desactivaba el resource shrinking optimizado de R8.

### Causa

Probablemente era un workaround conservador para builds release antiguas.

### Cómo Afecta

En debug apenas importa, pero en release puede impedir mejoras modernas del shrinker. Además, la propiedad está deprecated.

### Solución

Se eliminó la propiedad. La build release mantiene:

```kotlin
isMinifyEnabled = true
isShrinkResources = true
```

---

## 7. `android.defaults.buildfeatures.resvalues=true`

### Descripción

Forzaba el valor por defecto de `buildFeatures.resValues`.

### Causa

AGP está cambiando defaults para reducir trabajo innecesario en proyectos que no generan recursos desde Gradle.

### Cómo Afecta

Si un proyecto no usa `resValue(...)` en Gradle, mantenerlo activado solo conserva comportamiento antiguo y genera warning.

### Solución

Se eliminó la propiedad. El proyecto no define `resValue(...)` en `app/build.gradle.kts`.

---

## 8. `android.dependency.useConstraints` / `android.dependency.excludeLibraryComponentsFromConstraints`

### Descripción

AGP avisaba primero de que, si se mantenía `android.dependency.useConstraints=true`, convenía activar `android.dependency.excludeLibraryComponentsFromConstraints=true`. Al probarlo, AGP 9 mostró una recomendación más actual: usar `android.dependency.useConstraints=false`.

### Causa

El proyecto tenía activado:

```properties
android.dependency.useConstraints=true
```

Esa ruta de constraints es legacy para este proyecto pequeño y generaba trabajo extra durante sync/import.

### Cómo Afecta

No cambia la lógica de la app. Afecta al modelo de dependencias que Gradle/Android Studio calcula durante sync/build. La consecuencia práctica es menos ruido y mejor rendimiento de importación.

### Solución

Se cambió:

```properties
android.dependency.useConstraints=false
```

---

## Lección PBL

Un warning de build no siempre rompe la app hoy, pero sí es deuda técnica. En este caso, las advertencias indicaban que el proyecto dependía de interruptores antiguos del Android Gradle Plugin. La solución correcta no fue silenciar los mensajes, sino quitar las banderas que ya no aportaban valor y conservar solo la configuración explícita que el proyecto realmente necesita.

---

## 9. `window.statusBarColor` deprecated

### Descripción

Varias Activities asignaban el color de la barra de estado directamente:

```kotlin
window.statusBarColor = getColor(R.color.gradient_start)
```

### Causa

En Android moderno, el sistema empuja hacia edge-to-edge y la API directa de colorear la status bar queda marcada como deprecated.

### Cómo Afecta

No rompía la app. El impacto era ruido de compilación y llamadas deprecated repartidas por varias pantallas.

### Solución

Se centralizó la llamada en `EdgeToEdgeUtils.setStatusBarColor(...)` con una supresión local. Así se conserva el aspecto actual sin repetir warnings en cada Activity. Si más adelante se migra todo a edge-to-edge puro, el cambio queda concentrado en un helper.

---

## 10. `ChipGroup.setOnCheckedChangeListener` deprecated

### Descripción

Dashboard y Evolución usaban el listener antiguo de `ChipGroup`:

```kotlin
setOnCheckedChangeListener { _, checkedId -> ... }
```

### Causa

Material Components sustituyó ese listener por `setOnCheckedStateChangeListener`, que entrega una lista de chips seleccionados.

### Cómo Afecta

No rompía la selección de rangos, pero dejaba warnings y mantenía API antigua.

### Solución

Se cambió al listener nuevo:

```kotlin
setOnCheckedStateChangeListener { _, checkedIds ->
    val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
}
```

Como esos grupos son de selección única, usar el primer ID conserva el comportamiento.

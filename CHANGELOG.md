# Changelog - CBDCounter

Todos los cambios notables del proyecto se documentarán en este archivo.

El formato se basa en [Keep a Changelog](https://keepachangelog.com/es/1.0.0/),
y este proyecto adhiere a [Semantic Versioning](https://semver.org/lang/es/).

---

## [1.1.0] - 2025-11-10

### ✨ Añadido
- **Disclaimer médico obligatorio** en el primer uso de la app (requisito Google Play)
- **Política de privacidad completa** (RGPD + Google Play compatible)
- **Documentación para Play Store** (descripciones corta y larga)
- **GitHub Pages** con documentación oficial publicada
- **Configuración de producción completa** (ProGuard/R8, firma digital, AAB)

### 🐛 Corregido
- **Bug crítico:** Widget ahora respeta los emojis personalizados del usuario (usaba emojis hardcodeados)
- **Bug crítico:** Confirmación obligatoria antes de importar CSV (previene pérdida accidental de datos)
- **Mejora:** Código optimizado y ofuscado con R8 para reducir tamaño del APK

### 🔧 Cambios Técnicos
- Migrado de APK a **Android App Bundle (AAB)** (obligatorio desde 2021)
- Configurado **Google Play App Signing**
- Reglas **ProGuard/R8** específicas para la app
- **Target SDK actualizado a 34** (Android 14)
- Reducción del tamaño de release: 5.8MB → 3.5MB (~40% menor)

### 📋 Preparación para Play Store
- ✅ Cumple políticas de contenido de Google Play
- ✅ Disclaimer médico para apps relacionadas con CBD
- ✅ Sección de Seguridad de Datos lista
- ✅ Política de privacidad pública disponible

### 📚 Documentación
- Política de privacidad detallada (RGPD compliant)
- Instrucciones para GitHub Pages
- Descripciones para Play Store (corta + larga)
- Changelog estructurado

---

## [1.0.0] - 2025-09-29

### 🎉 Lanzamiento Inicial

Primer lanzamiento público de CBDCounter con todas las funcionalidades core.

### ✨ Funcionalidades
- **Contador diario** con botones +1, -1 y reset
- **Widget de pantalla principal** con actualización en tiempo real
- **Calendario visual** con navegación mensual
- **Sistema de notas** con timestamps automáticos
- **Estadísticas detalladas**: promedio, total, racha limpia
- **Filtros de visualización**: semana, mes, todo
- **Exportación/Importación CSV** para backups
- **Personalización de emojis**: 11 rangos con 151 emojis disponibles
- **Tema oscuro/claro** automático
- **Diferenciación de consumo**: estándar, con weed, con polen
- **Historial ilimitado** con persistencia local

### 🔒 Privacidad
- CERO recopilación de datos
- Almacenamiento 100% local (SharedPreferences)
- Sin analytics ni tracking
- Sin servicios de terceros

### 🎨 Diseño
- Material Design 3
- Interfaz intuitiva en español
- Emojis dinámicos según nivel de consumo
- Animaciones sutiles

### 📱 Compatibilidad
- Android 7.0 (API 24) y superior
- Optimizado para teléfonos y tablets
- Modo vertical y horizontal

### 🆓 Modelo
- Totalmente gratuita
- Sin anuncios
- Sin compras dentro de la app
- Código abierto (GPL-3.0)

---

## [Unreleased]

### 🚀 Próximas Funcionalidades (v1.2.0+)
- [ ] Internacionalización (inglés)
- [ ] Gráficas visuales de tendencias
- [ ] Backup automático en Google Drive
- [ ] Recordatorios programables
- [ ] Modo privacidad con PIN/huella
- [ ] Widgets adicionales (tamaños variados)
- [ ] Temas de color personalizables
- [ ] Exportación a PDF

---

## Tipos de Cambios

- **✨ Añadido** - Nuevas funcionalidades
- **🔧 Cambiado** - Cambios en funcionalidades existentes
- **❌ Deprecado** - Funcionalidades que se eliminarán pronto
- **🗑️ Eliminado** - Funcionalidades eliminadas
- **🐛 Corregido** - Corrección de bugs
- **🔒 Seguridad** - Parches de seguridad

---

[1.1.0]: https://github.com/D4vRAM369/CBDcounter/compare/v1.0...v1.1.0
[1.0.0]: https://github.com/D4vRAM369/CBDcounter/releases/tag/v1.0
[Unreleased]: https://github.com/D4vRAM369/CBDcounter/compare/v1.1.0...HEAD

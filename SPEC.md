# SPEC — Rediseño "Anillos del día"

> Estado: **listo para aprobación de Brian** (2026-10-25). Nada se construye hasta que lo apruebe.
> **[decisión de Claude]** = lo decidió Claude sin preguntar; Brian puede cambiarlo.

## Intención

Dar frescura visual a CBDcounter **sin cambiar la dinámica**: contador CBD/THC, +1, infusión (weed/polen), −1 que deshace la última toma real, emoji por nivel, hoja inferior con historial. Solo cambian la piel y la jerarquía de la pantalla principal.

## Decidido por Brian

1. **Dirección A · Anillos del día**: dos anillos concéntricos, CBD exterior y THC interior, que se llenan según las metas diarias. Emoji en el centro.
2. **Sin metas** (`goal_*_max == 0`): anillos tenues en reposo (solo la pista, sin relleno) y un toque suave que lleva a configurar metas, sin forzar: nada de diálogos ni banners insistentes. Cada anillo se evalúa por separado (puede haber meta CBD y no THC). Con meta 0 no se calcula fracción.
3. **Anillo lleno = "llegaste al límite"**, con aviso suave. Las metas son máximos, no objetivos. Sin sonidos, sin ✓ de logro, sin confeti.
4. **Widget fuera de la v1**: se rediseña en la Fase 5. No se toca ahora, pero debe seguir sincronizado con la app.
5. **Modo claro y oscuro en la v1**, respetando el botón de tema existente.

## Estado actual (punto de partida)

Material 3 morado; números `CBD / THC` y emoji centrados en un `FrameLayout`; 6 iconos en dos filas arriba (buscar, tema, ajustes, calendario, exportar, importar); hoja inferior `BottomSheetBehavior` (peek 260dp) con botones e historial. Archivo: `app/src/main/res/layout/activity_main.xml`; lógica en `MainActivity.kt`.

## Diseño

### Anillos

- Anillo exterior CBD, interior THC; fracción = conteo de hoy / meta.
- Emoji central con el mismo `EmojiUtils` (sin cambiar los niveles).
- Números debajo de cada anillo con su meta ("CBD · meta 5"); si no hay meta, solo el número.
- Escala de estados (colores **[decisión de Claude]**):
  - **Bajo el límite:** color propio (CBD verde, THC ámbar).
  - **En el 100%:** el anillo pasa a ámbar de aviso y muestra "En tu límite".
  - **Por encima:** rojo suave, segunda vuelta fina y "+N sobre tu meta".
  - **Sin meta:** pista tenue, sin relleno.
- Estado vacío (0 tomas): anillos vacíos y emoji de amanecer **[decisión de Claude, solo en vacío]**.

### Hoja inferior

- Botones: +1 CBD, 🌿 THC (infusión, mismo flujo actual), −1. Botones coloreados por tipo **[decisión de Claude]**.
- −1 apagado cuando no hay nada que deshacer (misma condición que usa hoy `IntakeController`).
- Historial debajo, sin cambios de datos.

### Barra superior

- **[decisión de Claude]** Solo 🔍 y ⋯. Calendario, exportar, importar, tema y ajustes pasan al menú ⋯. Todas las acciones actuales siguen disponibles.

### Temas

- Oscuro: verde bosque (ver mockups). Claro: se deriva con los mismos roles de color (pista, CBD, THC, aviso, exceso) y debe tener contraste legible. Definir los roles en `colors.xml`/temas, no valores sueltos.

### Mockups

`.superpowers/brainstorm/*/content/direcciones.html` y `anillos-estados.html` (locales, gitignored; solo oscuro). El mockup "Excedido" muestra un ✓ verde al llegar a la meta: **está desfasado**, vale la escala de arriba.

## Restricciones técnicas

- Vistas XML, sin Compose, sin librerías nuevas. **[decisión de Claude]** `RingsView` custom (patrón de `LineChartView`) con la lógica de progreso separada y testeable (fracción, estado, meta 0).
- No romper: `IntakeController`/undo (rama `feat/smart-undo-button`), sincronía con el widget, edge-to-edge, reset de medianoche.
- Accesibilidad: `contentDescription` de los anillos con valores y metas; el estado no depende solo del color (texto "En tu límite" / "+N").

## Lo que decide quien construya

Grosor y espaciado de los anillos, curva y duración de la animación de llenado, texto exacto del toque suave "sin metas" y a dónde lleva (`GoalsActivity`), tonos exactos del tema claro.

## No-objetivos

- No cambiar el modelo de datos ni el formato CSV/backup.
- No añadir funciones nuevas de consumo.
- No tocar el widget (Fase 5).

## Anti-objetivos

- Que llenar un anillo se sienta como un logro a perseguir o gamifique el consumo. Es una app de reducción de daños.
- Que el aviso de límite resulte alarmante o culpabilizante.

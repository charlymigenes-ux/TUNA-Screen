# Handoff — TUNA-Screen

Para quien (persona o agente) siga con este proyecto sin haber estado en la
conversación donde se armó. Contexto completo, no solo un TODO list.

## Qué es esto

Cliente Android (Kotlin + Jetpack Compose) para NOPAL (backend FastAPI en
`/home/jcjc/nopal`, repo separado). TUNA-Screen **nunca** habla directo con
Klipper/Moonraker/GRBL/Bambu/Elegoo/FlashForge — todo pasa por
`/api/tunascreen/*` + `/ws/tunascreen` en NOPAL. Ver `docs/TUNASCREEN_API.md`
**en el repo de NOPAL** — es el contrato canónico, escrito para que un
cliente nuevo no tenga que adivinar nada. Este archivo no repite ese
contrato, solo señala dónde está y qué considerar al usarlo.

## Cómo llegamos hasta acá (2 rondas)

1. **Ronda 1** (yo, sin JDK/Android SDK en mi entorno): escribí el scaffold
   original a mano — modelos, Retrofit/OkHttp, WebSocket con reconexión,
   pairing, DataStore, Discovery/Pairing/Taller/Máquina básicos. Se probó
   por primera vez en Android Studio real (Windows) y compiló y corrió
   contra un NOPAL real: pairing, WS en vivo, Home/Pausar/Reanudar/Cancelar
   confirmados funcionando.
2. **Ronda 2** (otro agente, con Android Studio real): expandió bastante
   más allá del MVP original — pantallas de dashboard (`DashboardScreens.kt`,
   `WorkshopComponents.kt`, `MachineComponents.kt`), diálogo de acciones
   (`MachineActionDialog.kt`), perfiles de densidad visual por tamaño de
   pantalla (`ui/theme/Responsive.kt` — 3.5"/4.3"/5"/7"/10"/desktop, ver
   `DisplayProfile`), tema personalizable (`AppearanceStore.kt`), splash
   screen con logo, modo inmersivo. También extendió el contrato del lado
   NOPAL: `driver` + `actions` (separado de `capabilities`) en cada
   `Machine`, `api_version`, `GET /api/tunascreen/config`, y le agregó a
   Klipper soporte real de home/mover/extruir reusando
   `klipper_service.send_console_command` (G28/G1 vía Moonraker, no un
   endpoint REST nuevo).

Yo (ronda 1) revisé el diff completo de la ronda 2 después del hecho:
backend con 235/235 tests pasando, contrato Android↔NOPAL consistente campo
por campo en los modelos (`Machine.kt`, `NopalServer.kt`). **No revisé línea
por línea las ~5000 líneas nuevas de UI Compose** (`MachineScreen.kt` son
648 líneas sola) — ojo ahí si aparecen bugs de layout raros, ya encontré y
corregí dos así en la ronda 1 (`Modifier.fillMaxSize()` donde debía ser
`fillMaxWidth()` en una Row, e inferencia de tipo de
`SavedStateHandle.get<String>()` que necesitó el argumento de tipo
explícito porque si no Kotlin no lo infiere solo).

## Bug real ya encontrado y corregido — cuidado si se toca de nuevo

`tunascreen_service.py` (NOPAL) tenía una condición de carrera: `_save_registry()`
escribía siempre al mismo nombre de archivo temporal
(`tunascreen_devices.json.tmp`). Con REST y WebSocket autenticando casi al
mismo tiempo (llaman a `resolve_device()`, que hace read-modify-write sobre
el registro), dos escritores podían pisarse el temporal mutuamente. Esto
**ya corrompió el registro real una vez** (quedó un `tunascreen_devices.json.corrupt-*.bak`
como evidencia). Ya está arreglado: `tempfile.NamedTemporaryFile` (un
temporal único por escritor) + `threading.RLock` alrededor de todo
load/save. Si se agrega OTRO archivo de estado mutable compartido entre
requests concurrentes en este backend, aplicar el mismo patrón desde el
principio, no como parche después de que se corrompa algo.

## Qué falta (a propósito, no por olvido)

- **Modo Máquina Avanzado real**: `AdvancedMachineScreen.kt` sigue siendo un
  shell — gráfica de temperaturas, jog XYZ fino, consola, macros,
  sensores/relés/LEDs, Spoolman, telemetría, todo pendiente.
- **Kiosk mode real**: `KioskModeManager.kt` es un no-op — falta
  `lockTask`/Device Owner, máquina asignada por dispositivo, arranque
  automático.
- **mDNS del lado NOPAL**: `NopalDiscoveryService.kt` (NSD/Android) ya
  funciona, pero NOPAL no anuncia `_nopal._tcp` en la red todavía (haría
  falta `python-zeroconf` en el startup de NOPAL). Hasta entonces, Discovery
  siempre cae al campo de IP manual — que anda bien.
- **UI de administración de dispositivos pareados**: el backend ya expone
  `GET/DELETE /api/tunascreen/devices` y la web de NOPAL (Configuración >
  TUNA-Screen) ya tiene botón de generar código + lista de dispositivos con
  opción de quitar. Del lado Android no hay pantalla para esto todavía (no
  hace falta — es un dispositivo viendo su propia sesión, no administrando
  otros).

## Convención del repo de NOPAL a respetar si se toca ese lado

- `backend/tests/conftest.py::isolated_printer_registries` (autouse) — CUALQUIER
  módulo nuevo con un `REGISTRY_PATH`-style constant respaldado por JSON
  tiene que agregarse ahí, si no los tests van a leer/escribir el archivo
  real del repo.
- `index.html` referencia `style.css`/`app.js`/`translations.js` con un
  query param `?v=N` para cache-busting — hay tests (`test_marlin_ui.py`,
  `test_material_preheat_ui.py`, `test_plugin_frontend_cache.py`) que
  verifican que ese número se haya bumpeado cuando el contenido cambia. Si
  se edita `app.js`/`style.css` y no se toca el número, esos tests fallan
  (ya pasó dos veces en esta sesión).
- `pip install -r requirements-dev.txt` antes de correr `pytest` si el
  entorno es nuevo (no viene preinstalado).

## Entorno de build

Ya compiló y corrió en Android Studio (Windows) contra un NOPAL real en la
misma red. El repo tiene `gradle-wrapper.jar` real (agregado en la ronda 2,
generado desde un Android Studio de verdad) — no hace falta regenerarlo.

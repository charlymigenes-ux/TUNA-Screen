# TUNA-Screen

Cliente Android de NOPAL construido con Kotlin y Jetpack Compose. La app es
la interfaz táctil; NOPAL sigue siendo el servidor que integra Klipper,
Marlin, GRBL, Bambu, Elegoo, FlashForge, cámaras y accesorios.

<img width="1586" height="992" alt="tunascreen" src="https://github.com/user-attachments/assets/b5341e6b-d3d8-4500-bce1-5f666bdeed59" />


TUNA-Screen nunca se conecta directamente al hardware. Toda lectura y acción
pasa por `/api/tunascreen/*` y `/ws/tunascreen`.

## Estado actual

- Intro NOPAL en pantalla completa y orientación horizontal.
- Android sin barra de estado ni barra de navegación durante el uso.
- Descubrimiento de NOPAL por NSD y conexión manual por IP.
- Emparejamiento con código temporal y token persistente.
- Taller, lista de máquinas, cámaras disponibles y ajustes.
- Tema oficial NOPAL, grafito y alto contraste.
- Selección automática o manual del perfil físico:
  - 3.5"/4.3": máquina esencial.
  - 5": máquina avanzada.
  - 7": máquina o multimáquina.
  - 10": taller.
  - PC: dashboard completo.
- Paneles distintos para impresora 3D, láser y CNC.
- Estado en vivo por WebSocket.
- Acciones habilitadas según `machine.actions`; no se simulan funciones que
  el driver de NOPAL no soporte.

## Contrato NOPAL versión 1

Cada máquina incluye:

- `capabilities`: información o función que la UI puede representar.
- `actions`: órdenes que el backend realmente puede ejecutar.
- `status`: telemetría normalizada.

Controles integrados:

- Comunes: pausar, reanudar y cancelar.
- Impresora: home, movimiento, extrusión y temperatura.
- Láser: home, movimiento, potencia y air assist.
- CNC: home, movimiento, cero de trabajo, husillo/RPM y refrigerante.

El documento canónico está en `docs/TUNASCREEN_API.md` del repositorio NOPAL.

## Compilar

El proyecto usa JDK 17 y Android SDK 35.

```powershell
.\gradlew.bat assembleDebug
```

APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Flujo de emparejamiento

1. Un administrador abre NOPAL y genera un código TUNA-Screen.
2. En Android se selecciona el servidor NOPAL.
3. La app canjea el código de seis dígitos por un token de dispositivo.
4. El token se conserva en DataStore hasta usar “Olvidar vínculo” en Ajustes.

# MetroSpot Turbo Download Engine

Integración directa sobre `DownloadUtil`/`ExoDownloadService` de MetroSpot v13.7.0.

## Qué cambió

- `DownloadManager.maxParallelDownloads` deja de estar fijo en 3.
- Wi‑Fi inicia con hasta 6 descargas paralelas.
- Datos móviles inicia con hasta 5.
- Otras redes inician con 3.
- La concurrencia se ajusta cada 4 segundos entre 2 y 6 según el rendimiento agregado observado.
- La preparación de URLs pasa de un `Semaphore(3)` a `Semaphore(6)`.
- El `DownloadManager` usa un executor fijo de 8 hilos en lugar de `Runnable::run`.
- Se agregó `downloadPlaylist(...)` para encolar una playlist sin que la UI tenga que serializar manualmente las canciones.
- Se agregó `downloadPlaylistItems(...)` para playlists remotas basadas en `SongItem`.
- Se conectaron las acciones de descarga de playlists existentes a estos métodos.
- Se conserva Media3 `DownloadManager`, `SimpleCache`, reanudación/rangos y el manejo de URLs expiradas ya existente.

## Por qué no se fuerza un número mayor

Más conexiones no garantizan más velocidad. El motor intenta utilizar más concurrencia cuando el throughput agregado mejora y reducirla cuando cae de forma significativa. Esto evita saturar el servidor, la red o el almacenamiento innecesariamente.

## Puntos modificados

- `app/src/main/kotlin/com/MetroSpot/music/playback/DownloadUtil.kt`
- `app/src/main/kotlin/com/MetroSpot/music/ui/menu/PlaylistMenu.kt`
- `app/src/main/kotlin/com/MetroSpot/music/ui/menu/YouTubePlaylistMenu.kt`
- `app/src/main/kotlin/com/MetroSpot/music/ui/screens/playlist/AutoPlaylistScreen.kt`
- `app/src/main/kotlin/com/MetroSpot/music/ui/screens/playlist/TopPlaylistScreen.kt`
- `app/src/main/kotlin/com/MetroSpot/music/ui/screens/playlist/CachePlaylistScreen.kt`

## Verificación

El código fue revisado estáticamente. La compilación Gradle no pudo ejecutarse en este entorno porque el wrapper necesita descargar Gradle 9.7.1 desde `services.gradle.org` y este entorno no tiene acceso de red saliente.

Para verificar localmente:

```bash
./gradlew :app:assembleDebug
```

Después probar una playlist de 20–100 canciones y observar:

- número de descargas simultáneas;
- MB/s agregados;
- recuperación tras perder/redes cambiar;
- reanudación de una descarga parcial;
- estado correcto de `DownloadCache`;
- reproducción mientras se descarga.


## Turbo v2 concurrency

- URL preparation semaphore: 12
- Download executor threads: 16
- Initial Wi-Fi parallel downloads: 12
- Initial cellular parallel downloads: 8
- Adaptive range: 2..12 active downloads

The engine deliberately caps active downloads at 12. Increasing thread count beyond the active-download cap does not itself create more network transfers; the executor provides scheduling capacity while Media3 controls actual concurrent downloads.

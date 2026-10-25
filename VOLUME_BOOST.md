# MetroSpot — Volume Boost

Se añadió un refuerzo de ganancia digital dentro del pipeline PCM de Media3.

## Control
- 0 dB: sin refuerzo adicional.
- +3 dB: refuerzo suave.
- +6 dB: refuerzo fuerte recomendado.
- +9 dB: refuerzo muy fuerte.
- +12 dB: modo máximo.

El ajuste se aplica en caliente y se combina con la normalización de loudness existente.
La ganancia se limita a +12 dB para evitar configuraciones excesivas. Como toda ganancia digital, si la señal original ya está cerca de 0 dBFS puede producir clipping; por eso +6 dB es un buen punto de partida.

## Implementación
`VolumeNormalizationAudioProcessor` suma la ganancia del usuario a la ganancia de normalización antes de procesar PCM 16/24/32-bit o float.

La preferencia persistente es `VolumeBoostDbKey` (`0f..12f`).

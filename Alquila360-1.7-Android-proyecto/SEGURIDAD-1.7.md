# Alquila360 1.7 - Beta de controles de seguridad

Cambios:
- Reautenticación tras 30 segundos fuera de la aplicación (requiere pruebas con selector de documentos e impresión).
- Pantalla oculta durante reautenticación.
- Eliminada exportación sin cifrar tanto en interfaz como en puente Android.
- Se conserva importación de copias antiguas para migración.
- Incremento de versionCode a 8.

IMPORTANTE: NO es versión lista para Play Store. Los datos siguen en localStorage sin cifrar. No se ha compilado ni probado en un dispositivo.

Pruebas obligatorias: primer desbloqueo, volver antes y después de 30 segundos, cancelar desbloqueo, exportar/importar cifrado, importar legado, generar PDF, actualización con datos ficticios y recuperación de copia.

No instalar sobre la versión que contiene datos reales sin una copia de seguridad restaurada y verificada en otro dispositivo.

# Alquila360 1.4 — seguridad beta (NO PUBLICAR TODAVÍA)

## Implementado
- Exportación e importación de copia cifrada mediante AES-256-GCM, salt e IV aleatorios y PBKDF2-HMAC-SHA256 (310000 iteraciones).
- El formato de la copia cifrada autentica el contenido; una contraseña errónea no restaura los datos.
- Se conserva la importación de copias antiguas sin cifrar para migración; el usuario debe dejar de usarlas para datos reales.
- Mantiene restricciones de WebView y exclusión de copias automáticas de Android de 1.3.1.

## Riesgos y pendientes obligatorios antes de Play Store
- Las contraseñas se introducen en cuadros prompt() y cruzan la interfaz JavaScript: sustituir por UI nativa protegida y revisar ciclo de vida de secretos.
- La base local sigue en WebView localStorage SIN cifrar. El cifrado de exportación no cifra el almacenamiento interno.
- No existe bloqueo biométrico/PIN propio ni temporizador de sesión.
- Falta prueba real de restauración, actualización sin pérdida de datos, pruebas de penetración y revisión de privacidad.
- Falta compilación de APK/AAB, firma release estable, Play App Signing y política de privacidad definitiva.
- Las copias antiguas en texto plano deben eliminarse de forma segura por el usuario tras verificar la migración.
- NO sustituir una instalación con datos reales sin una copia verificada.

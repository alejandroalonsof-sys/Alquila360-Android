# Alquila360 — Preparación para Google Play (beta, no publicar todavía)

## Medidas incluidas en este paquete
- Sin permiso INTERNET ni permisos de almacenamiento general en el manifiesto.
- Tráfico HTTP en claro deshabilitado.
- WebView: depuración deshabilitada; acceso universal a archivos y contenido externo bloqueado.
- Bloqueo de solicitudes de recursos externos en WebView (revisar con pruebas de impresión y PDF).
- Backup automático de Android y transferencia de datos excluidos. **Las copias manuales JSON siguen sin cifrar**.
- Selector de documentos de Android para importar y exportar, con límite de 10 MB.
- Código de versión aumentado a 4. Target SDK 35; confirmar requisitos vigentes en Play Console.

## Antes de publicar — pendiente
1. Probar instalación, alta/edición/borrado, filtros, PDF, importar/exportar, sin conexión, en varios Android.
2. Realizar revisión de seguridad de JavaScript, HTML, WebView y dependencias; pruebas de archivos JSON maliciosos y pérdida de datos.
3. Añadir copias cifradas con contraseña y recuperación ante corrupción; no cargar datos personales reales hasta verificar la protección.
4. Preparar política de privacidad **veraz**, publicada en URL pública y accesible desde la app.
5. Completar el formulario Seguridad de los datos de Play Console según comportamiento comprobado, incluyendo terceros y SDKs.
6. Crear paquete AAB de release firmado con clave de publicación protegida, preferentemente con Play App Signing. No subir claves ni contraseñas al repositorio.
7. Verificar titularidad de marca, ficha de Play Store, capturas, icono, correo de soporte, clasificación de contenido, acceso a la app, permisos y declaraciones.
8. Revisar requisitos vigentes de pruebas cerradas, cuenta de desarrollador y target SDK en Play Console.
9. Tener una política de conservación/eliminación de datos y aviso RGPD para los arrendadores; valorar revisión jurídica profesional.

**Nota:** una compilación correcta no demuestra cumplimiento de Google Play ni del RGPD.

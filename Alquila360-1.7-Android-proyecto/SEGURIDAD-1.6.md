# Alquila360 1.6 — control de acceso beta

- Se exige bloqueo seguro del dispositivo (PIN, patrón o contraseña) y confirmación al abrir la app.
- El WebView permanece invisible hasta la autenticación.
- Si se cancela la autenticación, se cierra la actividad.
- Se rechazan acciones del puente JS antes de la autenticación.
- Se mantiene el cifrado de copias exportadas de la versión anterior.

## Limitaciones importantes

- NO se ha cifrado todavía la base de datos local: sigue en WebView localStorage.
- NO se ha validado compilación ni ejecución en dispositivos reales.
- NO se debe instalar sobre una versión con datos reales sin copia externa comprobada.
- No hay bloqueo automático al volver desde segundo plano; pendiente de implementar y probar.
- No está lista para publicar en Google Play.

## Pruebas necesarias

1. En teléfono con bloqueo activo, abrir app, cancelar autenticación y verificar cierre.
2. Abrir y autenticar correctamente; comprobar todos los datos y funcionalidades.
3. En teléfono sin bloqueo seguro, verificar que exige configurar bloqueo.
4. Probar exportación cifrada e importación con contraseña errónea y correcta.
5. Probar actualización sobre instalación de prueba con datos ficticios.

# Alquila360 1.5 - endurecimiento de seguridad (BETA, sin validar)

Cambios de esta entrega:
- FLAG_SECURE para dificultar capturas de pantalla y miniaturas de tareas recientes. No impide fotografiar la pantalla ni protege frente a dispositivos comprometidos.
- WebView sin acceso general al sistema de archivos; los recursos internos siguen cargándose desde android_asset. Debe comprobarse que iconos y recibos funcionen.
- Visor de impresión sin JavaScript ni acceso a archivos.
- Estado de importación cifrada reiniciado al terminar/cancelar para evitar aplicar contraseñas antiguas.
- Restauración: se guarda el JSON validado en localStorage antes de cambiar el estado en memoria; si falla, conserva el estado anterior.
- Exportación sin cifrar señalizada como no recomendada, conservada por compatibilidad.

Pendiente antes de publicar:
1. Migrar datos internos desde WebView localStorage a almacenamiento cifrado con Android Keystore, con estrategia de migración y reversión verificada.
2. Revisar límites y esquema de los campos importados; probar copias corruptas, gigantes y antiguas.
3. Añadir bloqueo de acceso con biometría/PIN si se decide su política de recuperación.
4. Compilar, instalar en dispositivos distintos, probar impresión, importación/exportación, actualizaciones y reinicios.
5. Crear AAB release firmado con clave privada protegida (no subir clave al repositorio), política de privacidad pública, ficha de seguridad de datos y pruebas de Google Play.

ADVERTENCIA: no instalar sobre una versión con datos reales sin copia cifrada probada y copia alternativa externa. No se ha ejecutado compilación Android ni auditoría independiente.

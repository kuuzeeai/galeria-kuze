# Galería Kuze

App Android para ver las fotos y videos guardados en el servidor casero (miniserve),
desde el mismo Wi-Fi de casa. Nada se mueve ni se borra en el servidor: la app solo lee.

## Qué hace

- Pestaña **Fotos**: cuadrícula de 3 por fila, agrupada por día.
- Pestaña **Carpetas**: cada carpeta del servidor como un álbum.
- Miniaturas livianas generadas en el servidor (puerto 8081); las fotos originales solo se piden al abrirlas.
- Miniaturas en caché en el teléfono, hasta 500 MB.
- Visor con zoom (doble toque), deslizar a la anterior/siguiente y videos en streaming.
- Deslizar hacia abajo para refrescar.
- Ajustes guardados cifrados (URL, usuario y clave).

## Cómo se usa

1. Instalar la APK.
2. Abrir la app y escribir la dirección del servidor, el usuario y la clave.
3. Tocar **Probar conexión** y luego **Guardar y ver fotos**.

Si el celular viejo cambia de IP, se cambia solo en **Ajustes** dentro de la app.

## Cómo se compila

Cada push a `main` compila la APK en GitHub Actions. El archivo queda en la pestaña
**Actions** del repositorio, en el artefacto `galeria-kuze-debug`.

Servidor esperado: miniserve 0.35 con autenticación básica, en modo solo lectura.

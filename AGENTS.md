# galeria-kuze (app Android, Kotlin + Compose)
Repo: kuuzeeai/galeria-kuze, rama main. Se compila en GitHub Actions (.github/workflows/build.yml), NO en el celular.

## Cuando el usuario diga "continúa"
1. Lee PROGRESO.md: solo la sección 8 (reglas) y la 9 (pendientes). Lo demás es historia.
2. Corre: git pull; git status -sb; gh run list -L 3 (para ver si el último build pasó).
3. Trabaja los pendientes de la sección 9, de uno en uno.

## Reglas
- Responder en español, mensajes cortos. El usuario no programa.
- Las reglas de la sección 8 de PROGRESO.md mandan sobre todo lo demás.
- No cambiar diseño ni configuración sin preguntar.
- Nunca pedir ni mostrar claves.
- Avisar antes de borrar o sobrescribir algo.
- No usar pkill. No matar la sesión tmux opencode.
- Al terminar una tarea: commit y push para que compile en GitHub Actions; revisar gh run list y arreglar si falla. Nunca subir claves.

## Al lograr algo o al quedar a medias
- Actualizar PROGRESO.md (sección 9): qué se logró, qué falta, errores actuales. Pocas líneas, con el estado REAL (si el build falla, decirlo).

## Ahorrar tokens
- No leer archivos enteros: usar grep o sed -n con rangos de líneas.
- Una tarea a la vez. Sesiones cortas.

package com.kuze.galeria.data

/** Constantes compartidas. */
const val DEFAULT_URL = "http://192.168.101.36:8080"

/**
 * Un archivo de foto o video que vive en el servidor miniserve.
 *
 * @param name nombre del archivo, ej. "IMG_20261118_042827.jpg"
 * @param folder carpeta visible, ej. "Camera/2026" (vacío = raíz)
 * @param path ruta en el servidor, ej. "/Camera/IMG_20261118_042827.jpg"
 * @param url URL completa, lista para descargar
 * @param isVideo true si es un video
 * @param modified fecha de modificación en milisegundos (0 si no se pudo leer)
 */
data class Media(
    val name: String,
    val folder: String,
    val path: String,
    val url: String,
    val isVideo: Boolean,
    val modified: Long,
    /** true si el archivo vive en el teléfono (local) en vez del servidor. */
    val local: Boolean = false
) {
    val id: String get() = path
}

/** Una subcarpeta del servidor. */
data class FolderRef(
    val name: String,
    val path: String
)

/** Lo que devuelve leer una carpeta del servidor. */
data class DirListing(
    val folders: List<FolderRef>,
    val files: List<Media>
)

/** Error de usuario o clave incorrectos. */
class UnauthorizedException : Exception("Usuario o clave incorrectos")

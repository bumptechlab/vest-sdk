package book.util

import java.io.File
import java.io.IOException

fun File?.ensureFile() {
    if (this != null && !this.exists()) {
        this.parentFile.ensureDirectory()
        try {
            this.createNewFile()
        } catch (_: IOException) {
        }
    }
}

fun File?.ensureDirectory() {
    if (this != null && !this.exists()) {
        this.mkdirs()
    }
}


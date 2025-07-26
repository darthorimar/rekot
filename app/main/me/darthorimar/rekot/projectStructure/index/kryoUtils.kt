package me.darthorimar.rekot.projectStructure.index

import com.esotericsoftware.kryo.io.Output
import java.io.ByteArrayOutputStream

fun writeBytes(action: (Output) -> Unit): ByteArray {
    return ByteArrayOutputStream().use { stream ->
        Output(stream).use { output ->
            action(output)
            output.flush()
            stream.flush()
            stream.toByteArray()
        }
    }
}

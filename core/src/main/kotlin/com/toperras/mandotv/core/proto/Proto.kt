package com.toperras.mandotv.core.proto

import java.io.ByteArrayOutputStream

/*
 * Codificador/decodificador protobuf mínimo. Los protocolos de las teles solo
 * usan enteros, booleanos, textos, bytes y mensajes anidados, así que no hace
 * falta una librería completa (ni generar código a partir de .proto).
 */

/** Construye un mensaje protobuf campo a campo. */
class ProtoWriter {
    private val out = ByteArrayOutputStream()

    fun int(field: Int, value: Int): ProtoWriter = varint(field, value.toLong())

    fun bool(field: Int, value: Boolean): ProtoWriter = varint(field, if (value) 1 else 0)

    fun string(field: Int, value: String): ProtoWriter = bytes(field, value.toByteArray(Charsets.UTF_8))

    fun bytes(field: Int, value: ByteArray): ProtoWriter {
        writeVarint(((field shl 3) or WIRE_LENGTH).toLong())
        writeVarint(value.size.toLong())
        out.write(value)
        return this
    }

    fun message(field: Int, build: ProtoWriter.() -> Unit): ProtoWriter =
        bytes(field, ProtoWriter().apply(build).toByteArray())

    fun toByteArray(): ByteArray = out.toByteArray()

    private fun varint(field: Int, value: Long): ProtoWriter {
        writeVarint(((field shl 3) or WIRE_VARINT).toLong())
        writeVarint(value)
        return this
    }

    private fun writeVarint(value: Long) {
        out.write(encodeVarint(value))
    }

    companion object {
        const val WIRE_VARINT = 0
        const val WIRE_FIXED64 = 1
        const val WIRE_LENGTH = 2
        const val WIRE_FIXED32 = 5

        fun encodeVarint(value: Long): ByteArray {
            val buffer = ByteArrayOutputStream()
            var rest = value
            while (true) {
                if (rest and 0x7FL.inv() == 0L) {
                    buffer.write(rest.toInt())
                    return buffer.toByteArray()
                }
                buffer.write(((rest and 0x7F) or 0x80).toInt())
                rest = rest ushr 7
            }
        }
    }
}

/** Mensaje protobuf ya decodificado: campo → valores (Long o ByteArray). */
class ProtoMessage(private val fields: Map<Int, List<Any>>) {
    fun has(field: Int): Boolean = fields.containsKey(field)

    fun int(field: Int): Int? = (fields[field]?.firstOrNull() as? Long)?.toInt()

    fun bytes(field: Int): ByteArray? = fields[field]?.firstOrNull() as? ByteArray

    fun string(field: Int): String? = bytes(field)?.toString(Charsets.UTF_8)

    fun message(field: Int): ProtoMessage? = bytes(field)?.let { parse(it) }

    /** Lista de campos presentes (útil para registros de depuración). */
    val fieldNumbers: Set<Int> get() = fields.keys

    companion object {
        fun parse(data: ByteArray): ProtoMessage {
            val fields = mutableMapOf<Int, MutableList<Any>>()
            var position = 0

            fun readVarint(): Long {
                var result = 0L
                var shift = 0
                while (true) {
                    require(position < data.size) { "Varint incompleto" }
                    val byte = data[position++].toInt() and 0xFF
                    result = result or ((byte and 0x7F).toLong() shl shift)
                    if (byte and 0x80 == 0) return result
                    shift += 7
                    require(shift < 64) { "Varint demasiado largo" }
                }
            }

            while (position < data.size) {
                val tag = readVarint()
                val field = (tag ushr 3).toInt()
                when ((tag and 0x7).toInt()) {
                    ProtoWriter.WIRE_VARINT -> fields.getOrPut(field) { mutableListOf() }.add(readVarint())
                    ProtoWriter.WIRE_LENGTH -> {
                        val length = readVarint().toInt()
                        require(length >= 0 && position + length <= data.size) { "Longitud no válida" }
                        fields.getOrPut(field) { mutableListOf() }.add(data.copyOfRange(position, position + length))
                        position += length
                    }
                    ProtoWriter.WIRE_FIXED64 -> position += 8
                    ProtoWriter.WIRE_FIXED32 -> position += 4
                    else -> throw IllegalArgumentException("Tipo de campo desconocido en el campo $field")
                }
            }
            return ProtoMessage(fields)
        }
    }
}

package com.toperras.mandotv.core

import com.toperras.mandotv.core.androidtv.AndroidTvPairing
import com.toperras.mandotv.core.proto.ProtoMessage
import com.toperras.mandotv.core.proto.ProtoWriter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigInteger

class ProtoTest {
    @Test
    fun varintEncoding() {
        assertArrayEquals(byteArrayOf(0x01), ProtoWriter.encodeVarint(1))
        assertArrayEquals(byteArrayOf(0xAC.toByte(), 0x02), ProtoWriter.encodeVarint(300))
    }

    @Test
    fun nestedMessageRoundTrip() {
        val bytes = ProtoWriter()
            .int(1, 2)
            .int(2, 200)
            .message(10) { string(1, "atvremote"); string(2, "Mando TV") }
            .toByteArray()
        val parsed = ProtoMessage.parse(bytes)
        assertEquals(2, parsed.int(1))
        assertEquals(200, parsed.int(2))
        assertEquals("atvremote", parsed.message(10)?.string(1))
        assertEquals("Mando TV", parsed.message(10)?.string(2))
    }

    @Test
    fun keyPressMatchesKnownBytes() {
        // remote_key_inject{key_code=19 (arriba), direction=3 (SHORT)}
        val bytes = ProtoWriter().message(10) { int(1, 19); int(2, 3) }.toByteArray()
        assertArrayEquals(byteArrayOf(0x52, 0x04, 0x08, 0x13, 0x10, 0x03), bytes)
    }

    @Test
    fun exponentAsUnsignedBytes() {
        assertArrayEquals(byteArrayOf(0x01, 0x00, 0x01), AndroidTvPairing.unsignedBytes(BigInteger.valueOf(65537)))
        // Un módulo con el bit alto activo no debe llevar el 0x00 inicial de BigInteger.
        assertArrayEquals(byteArrayOf(0xFF.toByte()), AndroidTvPairing.unsignedBytes(BigInteger.valueOf(255)))
    }
}

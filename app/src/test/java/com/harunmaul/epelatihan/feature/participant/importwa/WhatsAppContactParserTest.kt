package com.harunmaul.epelatihan.feature.participant.importwa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class WhatsAppContactParserTest {

    @Test
    fun `parseText parses various Indonesian WhatsApp message formats`() {
        val sampleText = """
            Daftar Peserta Pelatihan:
            1. Budi Santoso - 08123456789
            2. Siti Rahma (08567891234)
            +62 812-9876-5432 : Ahmad Fauzi
            Dewi Lestari, 087811223344
            089911223344
        """.trimIndent()

        val parsed = WhatsAppContactParser.parseText(sampleText)
        println("PARSED COUNT: " + parsed.size)
        parsed.forEach { println("ITEM: ${it.name} | ${it.rawPhone} | ${it.normalizedPhone}") }
        assertEquals(5, parsed.size)

        assertEquals("Budi Santoso", parsed[0].name)
        assertEquals("628123456789", parsed[0].normalizedPhone)
        assertTrue(parsed[0].isValidPhone)

        assertEquals("Siti Rahma", parsed[1].name)
        assertEquals("628567891234", parsed[1].normalizedPhone)

        assertEquals("Ahmad Fauzi", parsed[2].name)
        assertEquals("6281298765432", parsed[2].normalizedPhone)

        assertEquals("Dewi Lestari", parsed[3].name)
        assertEquals("6287811223344", parsed[3].normalizedPhone)

        assertEquals("Peserta WA (3344)", parsed[4].name)
        assertEquals("6289911223344", parsed[4].normalizedPhone)
    }

    @Test
    fun `parseVCard parses single and multiple vcards correctly`() {
        val vcardData = """
            BEGIN:VCARD
            VERSION:3.0
            FN:Andi Pratama
            TEL;TYPE=CELL:+6281311223344
            END:VCARD
            BEGIN:VCARD
            VERSION:3.0
            N:Kurniawan;Rudi;;;
            TEL;TYPE=WORK:081988776655
            END:VCARD
        """.trimIndent()

        val parsed = WhatsAppContactParser.parseVCard(ByteArrayInputStream(vcardData.toByteArray()))

        assertEquals(2, parsed.size)
        assertEquals("Andi Pratama", parsed[0].name)
        assertEquals("6281311223344", parsed[0].normalizedPhone)

        assertEquals("Rudi Kurniawan", parsed[1].name)
        assertEquals("6281988776655", parsed[1].normalizedPhone)
    }
}

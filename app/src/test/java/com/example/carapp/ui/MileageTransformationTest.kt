package com.example.carapp.ui

import androidx.compose.ui.text.AnnotatedString
import com.example.carapp.ui.addcar.MileageTransformation
import org.junit.Assert.*
import org.junit.Test

class MileageTransformationTest {
    @Test fun `grouping preserves every cursor position`() {
        listOf("", "0", "999", "1000", "85000", "100000", "2147483647").forEach { raw ->
            val transformed = MileageTransformation.filter(AnnotatedString(raw))
            assertEquals(raw, transformed.text.text.replace(" ", ""))
            (0..raw.length).forEach { offset ->
                val displayOffset = transformed.offsetMapping.originalToTransformed(offset)
                assertTrue(displayOffset in 0..transformed.text.length)
                assertEquals(offset, transformed.offsetMapping.transformedToOriginal(displayOffset))
            }
            (0..transformed.text.length).forEach { offset ->
                assertTrue(transformed.offsetMapping.transformedToOriginal(offset) in 0..raw.length)
            }
        }
        assertEquals("85 000", MileageTransformation.filter(AnnotatedString("85000")).text.text)
    }
}

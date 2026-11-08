package com.dongpiandi.douyintv.ui.components

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CommentsPanelWidthTest {
    @Test
    fun alwaysTakesOneThirdOfTheScreen() {
        assertEquals(640f, commentsPanelWidth(1920.dp).value, 0.001f)
        assertEquals(426.6667f, commentsPanelWidth(1280.dp).value, 0.001f)
    }
}

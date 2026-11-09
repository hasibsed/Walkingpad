package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bluetooth.WalkingPadBleConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WalkingPad MC21", appName)
    }

    @Test
    fun `validate speed ranges for MC21`() {
        assertTrue(WalkingPadBleConstants.MIN_SPEED_KMH >= 0.5f)
        assertTrue(WalkingPadBleConstants.MAX_SPEED_KMH <= 12.0f)
        assertEquals(0x02.toByte(), WalkingPadBleConstants.OPCODE_SET_TARGET_SPEED)
        assertEquals(0x00.toByte(), WalkingPadBleConstants.OPCODE_REQUEST_CONTROL)
    }
}

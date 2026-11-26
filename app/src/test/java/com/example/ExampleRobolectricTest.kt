package com.example

import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("NutriLens", appName)
  }

  @Test
  fun `adaptive launcher icon inflates without error`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val icon = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    assertNotNull("Launcher icon must inflate cleanly", icon)
    assertTrue("Launcher icon must be AdaptiveIconDrawable on API 26+", icon is AdaptiveIconDrawable)
  }
}

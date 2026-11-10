package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun useAppContext() {
    // Context of the app under test.
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals(BuildConfig.APPLICATION_ID, appContext.packageName)
  }

  @Test
  fun testLauncherIconLoadsSuccessfully() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    val pm = appContext.packageManager
    val appInfo = pm.getApplicationInfo(appContext.packageName, 0)
    val icon = appInfo.loadIcon(pm)
    assertNotNull(icon)
    assertTrue("Icon must be an AdaptiveIconDrawable", icon is android.graphics.drawable.AdaptiveIconDrawable)
  }
}

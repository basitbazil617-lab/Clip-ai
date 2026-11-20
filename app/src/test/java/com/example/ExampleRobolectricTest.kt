package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.ui.GtaViClipAiApp
import com.example.ui.theme.GtaViClipAiTheme
import com.example.viewmodel.StudioContainer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appNameMatchesBranding() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GTA VI Clip AI", appName)
    }

    @Test
    fun headerDisplaysBrandAndCreatorProminently() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val container = StudioContainer(context)

        composeRule.setContent {
            GtaViClipAiTheme {
                GtaViClipAiApp(container = container)
            }
        }

        composeRule.onNodeWithTag("header_brand_title").assertIsDisplayed()
        composeRule.onNodeWithTag("header_creator_badge").assertIsDisplayed()
        composeRule.onNodeWithText("GTA VI CLIP AI").assertIsDisplayed()
        composeRule.onNodeWithText("MADE BY SAHIB").assertIsDisplayed()
        composeRule.onNodeWithTag("dashboard_screen").assertIsDisplayed()
    }
}

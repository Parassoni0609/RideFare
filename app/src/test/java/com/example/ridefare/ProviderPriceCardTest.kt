package com.example.ridefare

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.data.model.RideProvider
import com.example.ui.components.ProviderPriceCard
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProviderPriceCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `provider with no quote stays actionable without showing a numeric fare`() {
        var opened = 0
        compose.setContent { MaterialTheme { ProviderPriceCard(RideProvider.UBER) { opened++ } } }
        compose.onNodeWithText("Live price unavailable").assertIsDisplayed()
        compose.onAllNodes(hasText("₹", substring = true)).assertCountEquals(0)
        compose.onNodeWithTag("open_provider_UBER").performClick()
        assertEquals(1, opened)
    }
}

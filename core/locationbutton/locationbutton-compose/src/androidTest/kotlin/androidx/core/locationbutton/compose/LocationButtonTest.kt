/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.locationbutton.compose

import android.app.permissionui.LocationButtonRequest
import android.app.permissionui.LocationButtonSession
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.view.ContextThemeWrapper
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.core.locationbutton.R
import androidx.core.locationbutton.testing.TestLocationButtonProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationButtonTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testLocationButtonComposes() {
        composeTestRule.setContent {
            MaterialTheme {
                LocationButton(
                    modifier = Modifier.size(200.dp, 50.dp),
                    textType = LocationButtonTextType.PreciseLocation,
                    onRequestPermissions = {},
                ) {
                    // onPermissionResult
                }
            }
        }

        // If it reaches here without crashing, it passes!
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    fun testLocationButtonLocales_remoteSystemUI() {
        val capturedRequests = mutableListOf<LocationButtonRequest>()
        val provider =
            object :
                TestLocationButtonProvider(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ) {
                override fun onSessionRequestReceived(
                    request: LocationButtonRequest,
                    session: LocationButtonSession,
                ) {
                    capturedRequests.add(request)
                }
            }

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLocationButtonProvider provides provider) {
                    Column {
                        CompositionLocalProvider(
                            LocalConfiguration provides
                                Configuration().apply {
                                    setLocales(LocaleList(Locale.forLanguageTag("en-US")))
                                }
                        ) {
                            LocationButton(
                                modifier = Modifier.size(200.dp, 50.dp),
                                textType = LocationButtonTextType.PreciseLocation,
                                onRequestPermissions = {},
                            ) {
                                // onPermissionResult
                            }
                        }

                        CompositionLocalProvider(
                            LocalConfiguration provides
                                Configuration().apply {
                                    setLocales(LocaleList(Locale.forLanguageTag("fr-CA")))
                                }
                        ) {
                            LocationButton(
                                modifier = Modifier.size(200.dp, 50.dp),
                                textType = LocationButtonTextType.PreciseLocation,
                                onRequestPermissions = {},
                            ) {
                                // onPermissionResult
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(capturedRequests).hasSize(2)
        assertThat(capturedRequests[0].configuration.locales.get(0))
            .isEqualTo(Locale.forLanguageTag("en-US"))
        assertThat(capturedRequests[1].configuration.locales.get(0))
            .isEqualTo(Locale.forLanguageTag("fr-CA"))
    }

    @Composable
    private fun ProvideLocaleOverride(localeTag: String, content: @Composable () -> Unit) {
        val context = LocalContext.current
        val config =
            Configuration(LocalConfiguration.current).apply {
                setLocales(LocaleList(Locale.forLanguageTag(localeTag)))
            }
        val newContext =
            ContextThemeWrapper(context, 0).apply { applyOverrideConfiguration(config) }

        CompositionLocalProvider(
            LocalContext provides newContext,
            LocalConfiguration provides config,
        ) {
            content()
        }
    }

    @Test
    fun testLocationButtonLocales_localFallback() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN)

        composeTestRule.setContent {
            MaterialTheme {
                Column {
                    ProvideLocaleOverride("en-US") {
                        LocationButton(
                            modifier = Modifier.size(200.dp, 50.dp).testTag("LocationButton_en-US"),
                            textType = LocationButtonTextType.PreciseLocation,
                            onRequestPermissions = {},
                        ) {
                            // onPermissionResult
                        }
                    }
                    ProvideLocaleOverride("fr-CA") {
                        LocationButton(
                            modifier = Modifier.size(200.dp, 50.dp).testTag("LocationButton_fr-CA"),
                            textType = LocationButtonTextType.PreciseLocation,
                            onRequestPermissions = {},
                        ) {
                            // onPermissionResult
                        }
                    }
                }
            }
        }
        val enString = getLocalizedString("en-US", R.string.location_button_precise_location)
        val frString = getLocalizedString("fr-CA", R.string.location_button_precise_location)

        composeTestRule.waitForIdle()

        composeTestRule
            .onNode(hasText(enString) and hasAnyAncestor(hasTestTag("LocationButton_en-US")))
            .assertExists()
        composeTestRule
            .onNode(hasText(frString) and hasAnyAncestor(hasTestTag("LocationButton_fr-CA")))
            .assertExists()
    }

    private fun getLocalizedString(localeTag: String, @StringRes resId: Int): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config =
            Configuration(context.resources.configuration).apply {
                setLocales(LocaleList(Locale.forLanguageTag(localeTag)))
            }
        return context.createConfigurationContext(config).getString(resId)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.CINNAMON_BUN)
    @Test
    fun testLocationButtonRequest() {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN)
        var capturedRequest: LocationButtonRequest? = null
        val platformProvider =
            object :
                TestLocationButtonProvider(
                    InstrumentationRegistry.getInstrumentation().targetContext
                ) {
                override fun onSessionRequestReceived(
                    request: LocationButtonRequest,
                    session: LocationButtonSession,
                ) {
                    capturedRequest = request
                }
            }
        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalLocationButtonProvider provides platformProvider) {
                    LocationButton(
                        modifier = Modifier.size(200.dp, 50.dp),
                        cornerRadius = 12.dp,
                        textColor = Color.Red,
                        pressedCornerRadius = 6.dp,
                        clickablePadding =
                            PaddingValues(top = 6.dp, start = 5.dp, bottom = 7.dp, end = 8.dp),
                        onRequestPermissions = {},
                    ) {
                        // onPermissionResult
                    }
                }
            }
        }

        composeTestRule.waitForIdle()

        assertThat(capturedRequest).isNotNull()
        val request = capturedRequest!!
        assertThat(request.textType).isEqualTo(LocationButtonTextType.PreciseLocation.value)

        val density =
            InstrumentationRegistry.getInstrumentation()
                .targetContext
                .resources
                .displayMetrics
                .density
        assertThat(request.cornerRadius).isEqualTo(12f * density)
        assertThat(request.pressedCornerRadius).isEqualTo(6f * density)
        assertThat(request.textColor).isEqualTo(Color.Red.toArgb())

        val expectedLeftPx = Math.round(5f * density)
        val expectedTopPx = Math.round(6f * density)
        val expectedRightPx = Math.round(8f * density)
        val expectedBottomPx = Math.round(7f * density)

        assertThat(request.paddingLeft).isEqualTo(expectedLeftPx)
        assertThat(request.paddingTop).isEqualTo(expectedTopPx)
        assertThat(request.paddingRight).isEqualTo(expectedRightPx)
        assertThat(request.paddingBottom).isEqualTo(expectedBottomPx)
    }

    @Test
    fun testFallbackMode() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN)
        var clicked = false
        composeTestRule.setContent {
            MaterialTheme {
                LocationButton(
                    modifier = Modifier.testTag("FallbackButton").size(200.dp, 50.dp),
                    textType = LocationButtonTextType.PreciseLocation,
                    onRequestPermissions = { clicked = true },
                ) {
                    // onPermissionResult
                }
            }
        }

        composeTestRule.waitForIdle()

        // Verify fallback button is rendered and clickable!
        composeTestRule.onNodeWithTag("FallbackButton").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun testDefaultPermissionRequestBehavior() {
        assumeTrue(Build.VERSION.SDK_INT < Build.VERSION_CODES.CINNAMON_BUN)
        var launched = false
        var capturedPermissions: Array<out String>? = null

        val fakeRegistry =
            object : androidx.activity.result.ActivityResultRegistry() {
                @Suppress("UNCHECKED_CAST")
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: androidx.activity.result.contract.ActivityResultContract<I, O>,
                    input: I,
                    options: androidx.core.app.ActivityOptionsCompat?,
                ) {
                    launched = true
                    capturedPermissions = input as? Array<out String>
                }
            }
        val registryOwner =
            object : androidx.activity.result.ActivityResultRegistryOwner {
                override val activityResultRegistry = fakeRegistry
            }

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides
                        registryOwner
                ) {
                    LocationButton(
                        modifier = Modifier.testTag("FallbackButton").size(200.dp, 50.dp),
                        textType = LocationButtonTextType.PreciseLocation,
                    ) {
                        // onPermissionResult
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("FallbackButton").performClick()
        composeTestRule.waitForIdle()

        assertThat(launched).isTrue()
        assertThat(capturedPermissions).isNotNull()
        assertThat(capturedPermissions)
            .asList()
            .containsExactly(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
            )
    }
}

/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.testing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.LayoutElementBuilders.Layout
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Text
import androidx.wear.protolayout.expression.PlatformEventSources
import androidx.wear.protolayout.expression.PlatformHealthSources
import androidx.wear.protolayout.expression.intAppDataKey
import androidx.wear.protolayout.expression.mapTo
import com.google.common.truth.ExpectFailure
import com.google.common.truth.Truth.assertThat
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class LayoutElementAssertionsProviderTest {

    @Test
    fun primaryConstructor_onRoot() {
        assertTrue(LayoutElementAssertionsProvider(TEST_LAYOUT.root!!).onRoot().element is Box)
    }

    @Test
    fun secondaryConstructor_onRoot() {
        assertTrue(LayoutElementAssertionsProvider(TEST_LAYOUT).onRoot().element is Box)
    }

    @Test
    fun onRoot_errorMessageComposition() {
        val assertionError =
            assertThrows(AssertionError::class.java) {
                LayoutElementAssertionsProvider(TEST_LAYOUT).onRoot().assertDoesNotExist()
            }

        val rootDescription = "root"

        ExpectFailure.assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo("Expected $rootDescription to not exist, but it does.")
    }

    @Test
    fun onElement_isImage() {
        val firstImageElement =
            LayoutElementAssertionsProvider(TEST_LAYOUT).onElement(isImage).element as Image
        assertThat(firstImageElement.resourceId!!.value).isEqualTo("image1")
    }

    @Test
    fun onElement_isText() {
        val firstTextElement =
            LayoutElementAssertionsProvider(TEST_LAYOUT).onElement(isText).element as Text
        assertThat(firstTextElement.text!!.value).isEqualTo("text1")
    }

    @Test
    fun onElement_errorMessageComposition() {
        val assertionError =
            assertThrows(AssertionError::class.java) {
                LayoutElementAssertionsProvider(TEST_LAYOUT).onElement(isText).assertDoesNotExist()
            }

        val elementDescription = "element matching '${isText.description}'"

        ExpectFailure.assertThat(assertionError)
            .hasMessageThat()
            .isEqualTo("Expected $elementDescription to not exist, but it does.")
    }

    @Test
    fun injectedData_valueConfirmedInMatcher() {
        val intAppKey = intAppDataKey("intKey")
        val intAppValue = 13
        val dailySteps = 4567
        val visibility = true
        val provider =
            LayoutElementAssertionsProvider(TEST_LAYOUT)
                .withDynamicData(
                    intAppKey mapTo intAppValue,
                    PlatformHealthSources.Keys.DAILY_STEPS mapTo dailySteps,
                )
                .withDynamicData(PlatformEventSources.Keys.LAYOUT_VISIBILITY mapTo visibility)

        val appDataMatcher =
            LayoutElementMatcher("app data") { _, context ->
                context.dynamicData[intAppKey] == intAppValue
            }
        val platformDataMatcher =
            LayoutElementMatcher("platform data") { _, context ->
                context.dynamicData[PlatformHealthSources.Keys.DAILY_STEPS] == dailySteps
            }
        val platformEventMatcher =
            LayoutElementMatcher("platform event") { _, context ->
                context.dynamicData[PlatformEventSources.Keys.LAYOUT_VISIBILITY] == visibility
            }

        provider.onRoot().assert(appDataMatcher)
        provider.onRoot().assert(platformDataMatcher)
        provider.onRoot().assert(platformEventMatcher)
    }

    @Suppress("deprecation")
    companion object {
        val isImage = LayoutElementMatcher("Element type is Image") { element -> element is Image }
        val isText = LayoutElementMatcher("Element type is Text") { element -> element is Text }
        val TEST_LAYOUT =
            Layout.Builder()
                .setRoot(
                    Box.Builder()
                        .addContent(
                            Row.Builder()
                                .addContent(Image.Builder().setResourceId("image1").build())
                                .addContent(Image.Builder().setResourceId("image2").build())
                                .build()
                        )
                        .addContent(
                            Column.Builder()
                                .addContent(Text.Builder().setText("text1").build())
                                .addContent(Text.Builder().setText("text2").build())
                                .build()
                        )
                        .build()
                )
                .build()
    }
}

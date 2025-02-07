/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.protolayout.material3

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.hasClickable
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class PrimaryLayoutTest {
    @Test
    fun primaryLayout_addsContent_correctlyAdded() {
        val title = "Title"
        val label = "Label"
        val mainSlot = "Main slot"
        val bottomSlot = "Bottom slot"

        val provider =
            LayoutElementAssertionsProvider(
                materialScope(
                    context = ApplicationProvider.getApplicationContext(),
                    deviceConfiguration = DEVICE_PARAMETERS,
                ) {
                    primaryLayout(
                        titleSlot = { text(title.layoutString) },
                        mainSlot = { text(mainSlot.layoutString) },
                        bottomSlot = { text(bottomSlot.layoutString) },
                        onClick = CLICKABLE,
                        labelForBottomSlot = { text(label.layoutString) },
                    )
                }
            )

        provider.onElement(hasText(title)).assertExists()
        provider.onElement(hasText(label)).assertExists()
        provider.onElement(hasText(mainSlot)).assertExists()
        provider.onElement(hasText(bottomSlot)).assertExists()
        provider.onElement(hasClickable(CLICKABLE.onClick!!)).assertExists()
    }
}

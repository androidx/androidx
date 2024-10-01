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

package androidx.wear.protolayout.material3

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.material3.MaterialScopeTest.Companion.DEVICE_PARAMETERS
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class ButtonGroupTest {
    @Test
    fun buttonGroup_addsContent_correctlyAdded() {
        val element1 = Box.Builder().build()
        val element2 = Box.Builder().setWidth(expand()).build()

        val buttonGroup =
            primaryScope(
                context = ApplicationProvider.getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS
            ) {
                buttonGroup {
                    buttonGroupItem { element1 }
                    buttonGroupItem { element2 }
                }
            }

        assertThat((buttonGroup as Row).contents[0].toLayoutElementProto())
            .isEqualTo(element1.toLayoutElementProto())
        assertThat(buttonGroup.contents[2].toLayoutElementProto())
            .isEqualTo(element2.toLayoutElementProto())
    }
}

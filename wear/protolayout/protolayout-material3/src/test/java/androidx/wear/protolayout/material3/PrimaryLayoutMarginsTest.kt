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

import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.material3.PrimaryLayoutMargins.Companion.customizedPrimaryLayoutMargin
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class PrimaryLayoutMarginsTest {
    @Test
    fun customizedMargins_horizontal_buildsPadding() {
        val start = 0.2f
        val end = 0.3f
        val margins: CustomPrimaryLayoutMargins =
            customizedPrimaryLayoutMargin(start = start, end = end) as CustomPrimaryLayoutMargins

        assertThat(margins.toPadding(SCOPE).start!!.value)
            .isEqualTo(start * DEVICE_PARAMETERS.screenWidthDp)
        assertThat(margins.toPadding(SCOPE).end!!.value)
            .isEqualTo(end * DEVICE_PARAMETERS.screenWidthDp)
    }

    @Test
    fun customizedMargins_all_buildsPadding() {
        val start = 0.2f
        val end = 0.3f
        val bottom = 0.4f
        val margins: CustomPrimaryLayoutMargins =
            customizedPrimaryLayoutMargin(start = start, end = end, bottom = bottom)
                as CustomPrimaryLayoutMargins

        assertThat(margins.toPadding(SCOPE).start!!.value)
            .isEqualTo(start * DEVICE_PARAMETERS.screenWidthDp)
        assertThat(margins.toPadding(SCOPE).end!!.value)
            .isEqualTo(end * DEVICE_PARAMETERS.screenWidthDp)
        assertThat(margins.toPadding(SCOPE).bottom!!.value)
            .isEqualTo(bottom * DEVICE_PARAMETERS.screenWidthDp)
    }

    companion object {
        val SCOPE =
            MaterialScope(
                context = getApplicationContext(),
                deviceConfiguration = DEVICE_PARAMETERS,
                allowDynamicTheme = true,
                theme =
                    MaterialTheme(
                        colorScheme = dynamicColorScheme(context = getApplicationContext())
                    ),
                defaultTextElementStyle = TextElementStyle(),
                defaultIconStyle = IconStyle(),
                defaultBackgroundImageStyle = BackgroundImageStyle(),
                defaultAvatarImageStyle = AvatarImageStyle(),
                layoutSlotsPresence = LayoutSlotsPresence(),
                defaultProgressIndicatorStyle = ProgressIndicatorStyle()
            )
    }
}

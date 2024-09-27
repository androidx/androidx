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

import android.R
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.material3.DynamicMaterialTheme.getColorProp
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class DynamicMaterialThemeTest {
    @Test
    @Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    fun test_api34() {
        assertThat(DynamicMaterialTheme.tokenNameToResourceId).hasSize(ColorTokens.TOKEN_COUNT)

        assertThat(
                getColorProp(ApplicationProvider.getApplicationContext(), ColorTokens.SECONDARY)!!
                    .argb
            )
            .isEqualTo(
                ApplicationProvider.getApplicationContext<Context>()
                    .resources
                    .getColor(
                        R.color.system_secondary_fixed,
                        ApplicationProvider.getApplicationContext<Context>().theme
                    )
            )

        // Check that any unknown color token is returned as null.
        assertThat(getColorProp(ApplicationProvider.getApplicationContext(), -15)).isNull()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    fun test_apiLessThan34() {
        assertThat(DynamicMaterialTheme.tokenNameToResourceId).isEmpty()

        // Check that any token color is returned as null from Dynamic Theme.
        assertThat(getColorProp(ApplicationProvider.getApplicationContext(), ColorTokens.SECONDARY))
            .isNull()

        // Check that any unknown color token is also null.
        assertThat(getColorProp(ApplicationProvider.getApplicationContext(), -15)).isNull()
    }
}

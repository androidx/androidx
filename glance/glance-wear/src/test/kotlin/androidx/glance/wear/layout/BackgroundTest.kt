/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.glance.wear.layout

import androidx.glance.Modifier
import androidx.glance.findModifier
import androidx.glance.unit.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BackgroundTest {
    @Test
    fun canUseBackgroundModifier() {
        val modifier = Modifier.background(color = Color(0xFF223344u))

        val addedModifier = requireNotNull(modifier.findModifier<BackgroundModifier>())

        assertThat(addedModifier.color.value).isEqualTo(0xFF223344u)
    }
}

/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.glance.semantics

import androidx.glance.GlanceModifier
import androidx.glance.findModifier
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SemanticsTest {
    @Test
    fun testModifier() {
        val modifiers = GlanceModifier.semantics({ contentDescription = "test_description" })

        val semanticsModifier = checkNotNull(modifiers.findModifier<SemanticsModifier>())
        assertThat(
            semanticsModifier.configuration
                .get(SemanticsProperties.ContentDescription).joinToString())
            .isEqualTo("test_description")
    }
}
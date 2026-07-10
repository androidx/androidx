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

package androidx.car.app.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/** Tests for [Shape]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class ShapeTest {

    @Test
    fun equals() {
        assertThat(Shape.CORNER_FULL).isEqualTo(Shape.CORNER_FULL)
        assertThat(Shape.CORNER_FULL).isNotEqualTo(Shape.NONE)
    }

    @Test
    fun hashCode_match() {
        assertThat(Shape.CORNER_FULL.hashCode()).isEqualTo(Shape.CORNER_FULL.hashCode())
        assertThat(Shape.CORNER_FULL.hashCode()).isNotEqualTo(Shape.NONE.hashCode())
    }
}

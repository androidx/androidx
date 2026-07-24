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
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
@DoNotInstrument
class TabStyleTest {
    @Test
    fun setShape() {
        val shape = Shape.CORNER_MEDIUM
        val style = TabStyle.Builder().setShape(shape).build()
        assertThat(style.shape).isEqualTo(shape)
    }

    @Test
    fun build_noFieldsSet_throws() {
        assertThrows(IllegalStateException::class.java) { TabStyle.Builder().build() }
    }

    @Test
    fun equals() {
        val shape = Shape.CORNER_EXTRA_LARGE

        val style1 = TabStyle.Builder().setShape(shape).build()
        val style2 = TabStyle.Builder().setShape(shape).build()
        val style3 = TabStyle.Builder().setShape(Shape.NONE).build()

        assertThat(style1).isEqualTo(style2)
        assertThat(style1).isNotEqualTo(style3)
    }

    @Test
    fun hashCode_match() {
        val shape = Shape.CORNER_EXTRA_LARGE

        val style1 = TabStyle.Builder().setShape(shape).build()
        val style2 = TabStyle.Builder().setShape(shape).build()
        val style3 = TabStyle.Builder().setShape(Shape.NONE).build()

        assertThat(style1.hashCode()).isEqualTo(style2.hashCode())
        assertThat(style1.hashCode()).isNotEqualTo(style3.hashCode())
    }
}

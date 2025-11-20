/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.authoring.internal

import android.graphics.Matrix
import androidx.ink.geometry.ImmutableVec
import androidx.ink.geometry.MutableBox
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class MutableBoxTransformTest {

    private val floatTolerance = 0.001F

    fun transform_whenIdentity_resultMatchesOriginal() {
        val rect = MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        rect.transform(Matrix())

        assertThat(rect)
            .isEqualTo(
                MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))
            )
    }

    @Test
    fun transform_whenScale_scalesBounds() {
        val rect = MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        rect.transform(Matrix().apply { setScale(5F, -6F) })

        assertThat(rect)
            .isEqualTo(
                MutableBox().populateFromTwoPoints(ImmutableVec(5F, -12F), ImmutableVec(15F, -24F))
            )
    }

    @Test
    fun transform_whenOffset_offsetsBounds() {
        val rect = MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        rect.transform(Matrix().apply { setTranslate(5F, -6F) })

        assertThat(rect)
            .isEqualTo(
                MutableBox().populateFromTwoPoints(ImmutableVec(6F, -4F), ImmutableVec(8F, -2F))
            )
    }

    @Test
    fun transform_whenRotation_newBoundsIncludeRotatedRect() {
        val rect = MutableBox().populateFromTwoPoints(ImmutableVec(0F, 0F), ImmutableVec(4F, 3F))

        rect.transform(Matrix().apply { setRotate(-36.87F) })

        assertThat(rect.xMin).isWithin(floatTolerance).of(0F)
        assertThat(rect.yMin).isWithin(floatTolerance).of(-2.4F)
        assertThat(rect.xMax).isWithin(floatTolerance).of(5F)
        assertThat(rect.yMax).isWithin(floatTolerance).of(2.4F)
    }

    @Test
    fun transform_whenDestinationSupplied_originalIsUnchanged() {
        val rect = MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))

        val dest = MutableBox().populateFromTwoPoints(ImmutableVec(0F, 0F), ImmutableVec(0F, 0F))

        rect.transform(Matrix().apply { setScale(5F, -6F) }, dest)

        assertThat(rect)
            .isEqualTo(
                MutableBox().populateFromTwoPoints(ImmutableVec(1F, 2F), ImmutableVec(3F, 4F))
            )
        assertThat(dest)
            .isEqualTo(
                MutableBox().populateFromTwoPoints(ImmutableVec(5F, -12F), ImmutableVec(15F, -24F))
            )
    }
}

/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.geometry

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SizeTest {

    @Test
    fun sizeTimesInt() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(2.5f, 2.5f) * 4f
        )
        Assert.assertEquals(
            Size(10f, 10f),
            4f * Size(2.5f, 2.5f)
        )
    }

    @Test
    fun sizeDivInt() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(40f, 40f) / 4f
        )
    }

    @Test
    fun sizeTimesFloat() {
        Assert.assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        Assert.assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivFloat() {
        Assert.assertEquals(Size(10f, 10f), Size(40f, 40f) / 4f)
    }

    @Test
    fun sizeTimesDouble() {
        Assert.assertEquals(Size(10f, 10f), Size(4f, 4f) * 2.5f)
        Assert.assertEquals(Size(10f, 10f), 2.5f * Size(4f, 4f))
    }

    @Test
    fun sizeDivDouble() {
        Assert.assertEquals(
            Size(10f, 10f),
            Size(40f, 40f) / 4.0f
        )
    }
}
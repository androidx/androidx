/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.util

import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import android.util.Pair as AndroidPair
import kotlin.Pair as KotlinPair

@SmallTest
class PairTest {
    @Test fun androidDestructuringNonNull() {
        val pair = AndroidPair("one", "two")
        val (first: String, second: String) = pair
        assertSame(pair.first, first)
        assertSame(pair.second, second)
    }

    @Test fun androidDestructuringNullable() {
        val pair = AndroidPair("one", "two")
        val (first: String?, second: String?) = pair
        assertSame(pair.first, first)
        assertSame(pair.second, second)
    }

    @Test fun androidToKotlin() {
        val android = AndroidPair("one", "two")
        val kotlin = android.toKotlinPair()
        assertEquals(android.first to android.second, kotlin)
    }

    @Test fun kotlinToAndroid() {
        val kotlin = KotlinPair("one", "two")
        val android = kotlin.toAndroidPair()
        assertEquals(AndroidPair(kotlin.first, kotlin.second), android)
    }

    @Test fun androidXDestructuringNonNull() {
        val pair = Pair("one", "two")
        val (first: String, second: String) = pair
        assertSame(pair.first, first)
        assertSame(pair.second, second)
    }

    @Test fun androidXDestructuringNullable() {
        val pair = Pair("one", "two")
        val (first: String?, second: String?) = pair
        assertSame(pair.first, first)
        assertSame(pair.second, second)
    }

    @Test fun androidXToKotlin() {
        val pair = Pair("one", "two")
        val kotlin = pair.toKotlinPair()
        assertEquals(pair.first to pair.second, kotlin)
    }

    @Test fun kotlinToAndroidX() {
        val kotlin = KotlinPair("one", "two")
        val pair = kotlin.toAndroidXPair()
        assertEquals(Pair(kotlin.first, kotlin.second), pair)
    }
}

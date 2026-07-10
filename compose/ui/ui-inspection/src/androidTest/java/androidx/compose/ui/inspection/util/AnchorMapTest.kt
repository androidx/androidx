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

package androidx.compose.ui.inspection.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AnchorMapTest {
    @Test
    fun testMap() {
        val map = AnchorMap()
        val anchor0 = Anchor(0)
        val anchor1 = Anchor(1)
        val anchor2 = Anchor(2)
        val anchor3 = Anchor(3)
        val anchor4 = Anchor(1)
        check(map, anchor0, 1, 117)
        check(map, anchor1, 2, 118)
        check(map, anchor2, 3, 217)
        check(map, anchor3, 4, 218)
        check(map, anchor4, 5, 317)
        check(map, anchor0, 1, 117)
        check(map, anchor1, 2, 118)
        check(map, anchor2, 3, 217)
        check(map, anchor3, 4, 218)
        check(map, anchor4, 5, 317)
    }

    private fun check(map: AnchorMap, anchor: Anchor, expectedId: Int, expectedKey: Int) {
        val id = map[anchor, expectedKey]
        assertThat(id).isEqualTo(expectedId)
        assertThat(map[id]).isSameInstanceAs(anchor)
        assertThat(map.getKey(anchor)).isEqualTo(expectedKey)
    }

    private class Anchor(private val hash: Int) {
        override fun hashCode(): Int {
            return hash
        }

        override fun equals(other: Any?): Boolean = this === other
    }
}

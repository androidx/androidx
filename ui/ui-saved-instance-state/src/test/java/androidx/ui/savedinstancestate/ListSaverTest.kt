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

package androidx.ui.savedinstancestate

import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class ListSaverTest {

    @Test
    fun simpleSaveAndRestore() {
        val original = Size(2, 3)
        val saved = with(SizeSaver) {
            allowingScope.save(original)
        }

        assertThat(saved).isNotNull()
        assertThat(SizeSaver.restore(saved!!))
            .isEqualTo(original)
    }

    @Test(expected = IllegalArgumentException::class)
    fun exceptionWhenAllItemsCantBeSaved() {
        with(SizeSaver) {
            disallowingScope.save(Size(2, 3))
        }
    }
}

private data class Size(val x: Int, val y: Int)

private val SizeSaver = listSaver<Size, Int>(
    save = { listOf(it.x, it.y) },
    restore = { Size(it[0], it[1]) }
)

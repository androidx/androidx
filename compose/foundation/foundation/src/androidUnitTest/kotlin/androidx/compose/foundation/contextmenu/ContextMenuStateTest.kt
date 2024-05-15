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

package androidx.compose.foundation.contextmenu

import androidx.compose.foundation.contextmenu.ContextMenuState.Status
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test

@Suppress("SENSELESS_COMPARISON")
class ContextMenuStateTest {
    private val openZero = ContextMenuState(Status.Open(Offset.Zero))
    private val openOne = ContextMenuState(Status.Open(Offset(1f, 1f)))

    @Test
    fun whenStatusOpen_unspecifiedOffset_throws() {
        assertFailsWith(IllegalStateException::class, "unspecified offset") {
            Status.Open(Offset.Unspecified)
        }
    }

    @Test
    fun whenStatusOpen_equalsNull_false() {
        assertThat(openZero == null).isFalse()
    }

    @Test
    fun whenStatusOpen_equalsSame_true() {
        assertThat(openZero == openZero).isTrue()
    }

    @Test
    fun whenStatusOpen_equalsOther_withDifferentValue_false() {
        assertThat(openZero == openOne).isFalse()
    }

    @Test
    fun whenStatusOpen_equalsOtherType_false() {
        assertThat(openZero.equals("String")).isFalse()
    }

    @Test
    fun whenStatusOpen_equalsOther_withSameValue_true() {
        val equalState = ContextMenuState(Status.Open(Offset(1f, 1f)))
        assertThat(openOne == equalState).isTrue()
    }

    @Test
    fun whenClose_statusIsClosed() {
        val state = ContextMenuState(Status.Open(Offset.Zero))
        assertThat(state.status).isInstanceOf(Status.Open::class.java)
        state.close()
        assertThat(state.status).isInstanceOf(Status.Closed::class.java)
    }
}

/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.content.createClipData
import androidx.compose.foundation.text.input.internal.selection.ClipboardPasteState
import androidx.compose.foundation.text.input.internal.selection.FakeClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

@MediumTest
@RunWith(AndroidJUnit4::class)
class ClipboardPasteStateTest {

    @Test
    fun clipboardPasteState_updates() = runTest {
        val clipboard = FakeClipboard()
        val clipboardPasteState = ClipboardPasteState(clipboard)

        clipboardPasteState.update()
        assertThat(clipboardPasteState.hasText).isFalse()
        assertThat(clipboardPasteState.hasClip).isFalse()

        clipboard.setClipEntry(createClipData { addText("hello") }.toClipEntry())
        clipboardPasteState.update()

        assertThat(clipboardPasteState.hasText).isTrue()
        assertThat(clipboardPasteState.hasClip).isTrue()
    }

    @Test
    fun clipboardPasteState_initiallyFalse() = runTest {
        val clipboard = FakeClipboard("hello, world")
        val clipboardPasteState = ClipboardPasteState(clipboard)

        // do not call update
        assertThat(clipboardPasteState.hasText).isFalse()
        assertThat(clipboardPasteState.hasClip).isFalse()
    }

    @Test
    fun clipboardPasteState_update_doesNotReadPrimaryClip() = runTest {
        val clipboard = FakeClipboard("hello, world")
        val clipboardPasteState = ClipboardPasteState(clipboard)

        clipboardPasteState.update()

        assertThat(clipboard.getClipEntryCalled).isEqualTo(0)
        verify(clipboard.nativeClipboard, atLeastOnce()).hasPrimaryClip()
        verify(clipboard.nativeClipboard, never()).primaryClip
    }
}

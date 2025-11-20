/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.text.input

import android.content.ClipDescription
import android.net.Uri
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputContentInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@SmallTest
@RunWith(AndroidJUnit4::class)
class NullableInputConnectionWrapperTest {

    private var delegate = mock<InputConnection>()

    @Test
    fun delegatesToDelegate() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})
        ic.setSelection(4, 2)
        verify(delegate).setSelection(4, 2)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun delegatesCloseConnectionToDelegate() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})
        ic.closeConnection()
        verify(delegate).closeConnection()
    }

    @SdkSuppress(minSdkVersion = 25)
    @Test
    fun delegatesCommitContentToDelegate() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})
        val contentInfo =
            InputContentInfo(Uri.parse("content://example.com"), ClipDescription("", emptyArray()))
        ic.commitContent(contentInfo, 42, null)
        verify(delegate).commitContent(contentInfo, 42, null)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun closeConnectionInvokesCallback() {
        var closeCalls = 0
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = { closeCalls++ })
        assertThat(closeCalls).isEqualTo(0)

        ic.closeConnection()
        assertThat(closeCalls).isEqualTo(1)
    }

    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun multipleCloseConnectionsInvokesCallbackOnlyOnce() {
        var closeCalls = 0
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = { closeCalls++ })
        assertThat(closeCalls).isEqualTo(0)

        repeat(5) { ic.closeConnection() }
        assertThat(closeCalls).isEqualTo(1)
    }

    @Test
    fun disposeDelegateDoesNotInvokeCallback() {
        var closeCalls = 0
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = { closeCalls++ })
        assertThat(closeCalls).isEqualTo(0)

        ic.disposeDelegate()
        assertThat(closeCalls).isEqualTo(0)
    }

    @Test
    fun stopsDelegatingAfterDisposal() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})

        ic.disposeDelegate()
        ic.setSelection(4, 2)

        verify(delegate, never()).setSelection(any(), any())
    }

    @Test
    fun getSelectedTextReturnsNull_whenDelegateIsDisposed() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})

        ic.disposeDelegate()
        val result = ic.getSelectedText(0)

        verify(delegate, never()).getSelectedText(any())
        assertThat(result).isNull()
    }

    @Test
    fun getSelectedTextReturnsNull_whenDelegateReturnsNull() {
        val ic = NullableInputConnectionWrapper(delegate, onConnectionClosed = {})

        val result = ic.getSelectedText(0)

        verify(delegate, times(1)).getSelectedText(any())
        assertThat(result).isNull()
    }
}

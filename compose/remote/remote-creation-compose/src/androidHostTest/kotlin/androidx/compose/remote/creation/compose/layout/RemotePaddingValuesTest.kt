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
package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.creation.compose.state.RemoteDp
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat.Companion.createNamedRemoteFloat
import androidx.compose.remote.creation.compose.state.rdp
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemotePaddingValuesTest {

    @Test
    fun equals_constantValues_areEqual() {
        val oneZero = RemotePaddingValues(0.rdp)
        val twoZero = RemotePaddingValues(0.rdp)
        assertThat(oneZero).isEqualTo(twoZero)
    }

    @Test
    fun equals_nonConstantValues_areNotEqual() {
        val namedRemoteFloat = createNamedRemoteFloat("testFloat", defaultValue = 100.0f)
        val nonConstantRemoteFloat = namedRemoteFloat * RemoteFloat(10f)
        assertThat(nonConstantRemoteFloat.hasConstantValue).isFalse()

        val padding1 = RemotePaddingValues(RemoteDp(nonConstantRemoteFloat))
        val padding2 = RemotePaddingValues(RemoteDp(nonConstantRemoteFloat))
        assertThat(padding1).isNotEqualTo(padding2)
    }

    @Test
    fun init_negativeConstantValue_throwsIllegalArgumentException() {
        val exception = assertFailsWith<IllegalArgumentException> { RemotePaddingValues((-10).rdp) }
        assertThat(exception).hasMessageThat().contains("Start padding must be non negative")
    }

    @Test
    fun init_negativeStartPadding_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                RemotePaddingValues((-10).rdp, 10.rdp, 10.rdp, 10.rdp)
            }
        assertThat(exception).hasMessageThat().contains("Start padding must be non negative")
    }

    @Test
    fun init_negativeTopPadding_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                RemotePaddingValues(10.rdp, (-10).rdp, 10.rdp, 10.rdp)
            }
        assertThat(exception).hasMessageThat().contains("Top padding must be non negative")
    }

    @Test
    fun init_negativeEndPadding_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                RemotePaddingValues(10.rdp, 10.rdp, (-10).rdp, 10.rdp)
            }
        assertThat(exception).hasMessageThat().contains("End padding must be non negative")
    }

    @Test
    fun init_negativeBottomPadding_throwsIllegalArgumentException() {
        val exception =
            assertFailsWith<IllegalArgumentException> {
                RemotePaddingValues(10.rdp, 10.rdp, 10.rdp, (-10).rdp)
            }
        assertThat(exception).hasMessageThat().contains("Bottom padding must be non negative")
    }

    @Test
    fun init_singleArgument() {
        val all = 10.rdp
        val remotePaddingValues = RemotePaddingValues(all)
        assertThat(remotePaddingValues.endPadding).isEqualTo(all)
        assertThat(remotePaddingValues.topPadding).isEqualTo(all)
        assertThat(remotePaddingValues.bottomPadding).isEqualTo(all)
        assertThat(remotePaddingValues.startPadding).isEqualTo(all)
    }

    @Test
    fun init_multipleArguments() {
        val start = 1.rdp
        val top = 2.rdp
        val end = 3.rdp
        val bottom = 4.rdp
        val remotePaddingValues = RemotePaddingValues(start, top, end, bottom)
        assertThat(remotePaddingValues.endPadding).isEqualTo(end)
        assertThat(remotePaddingValues.topPadding).isEqualTo(top)
        assertThat(remotePaddingValues.bottomPadding).isEqualTo(bottom)
        assertThat(remotePaddingValues.startPadding).isEqualTo(start)
    }

    @Test
    fun init_horizontalAndVerticalArguments() {
        val horizontal = 10.rdp
        val vertical = 20.rdp
        val remotePaddingValues = RemotePaddingValues(horizontal, vertical)
        assertThat(remotePaddingValues.endPadding).isEqualTo(horizontal)
        assertThat(remotePaddingValues.startPadding).isEqualTo(horizontal)
        assertThat(remotePaddingValues.topPadding).isEqualTo(vertical)
        assertThat(remotePaddingValues.bottomPadding).isEqualTo(vertical)
    }
}

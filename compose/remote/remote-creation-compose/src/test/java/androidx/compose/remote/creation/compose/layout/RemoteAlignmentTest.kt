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

package androidx.compose.remote.creation.compose.layout

import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.ui.unit.LayoutDirection
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RemoteAlignmentTest {

    @Test
    fun toRemoteLayoutDirection_convertsLtrAndRtl() {
        assertThat(LayoutDirection.Ltr.toRemoteLayoutDirection())
            .isEqualTo(RemoteLayoutDirection.Ltr)
        assertThat(LayoutDirection.Rtl.toRemoteLayoutDirection())
            .isEqualTo(RemoteLayoutDirection.Rtl)
    }

    @Test
    fun horizontalAlignment_toRemoteWithRemoteLayoutDirection() {
        assertThat(RemoteAlignment.Start.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteAlignment.Start.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.END)
        assertThat(RemoteAlignment.End.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.END)
        assertThat(RemoteAlignment.End.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteAlignment.CenterHorizontally.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.CENTER)
        assertThat(RemoteAlignment.CenterHorizontally.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.CENTER)
    }

    @Test
    fun absoluteHorizontalAlignment_ignoresLayoutDirection() {
        assertThat(RemoteAbsoluteAlignment.Left.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteAbsoluteAlignment.Left.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteAbsoluteAlignment.Right.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.END)
        assertThat(RemoteAbsoluteAlignment.Right.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.END)
    }

    @Test
    fun horizontalArrangement_toRemoteWithRemoteLayoutDirection() {
        assertThat(RemoteArrangement.Start.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteArrangement.Start.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.END)
        assertThat(RemoteArrangement.End.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.END)
        assertThat(RemoteArrangement.End.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteArrangement.Center.toRemote(RemoteLayoutDirection.Ltr))
            .isEqualTo(ColumnLayout.CENTER)
        assertThat(RemoteArrangement.Center.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.CENTER)
        assertThat(RemoteArrangement.Absolute.Left.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.START)
        assertThat(RemoteArrangement.Absolute.Right.toRemote(RemoteLayoutDirection.Rtl))
            .isEqualTo(ColumnLayout.END)
        assertThat(shouldReverse(RemoteArrangement.Start, RemoteLayoutDirection.Ltr)).isFalse()
        assertThat(shouldReverse(RemoteArrangement.Start, RemoteLayoutDirection.Rtl)).isTrue()
        assertThat(shouldReverse(RemoteArrangement.Absolute.Left, RemoteLayoutDirection.Rtl))
            .isFalse()
    }
}

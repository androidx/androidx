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

package androidx.glance.appwidget.remotecompose

import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests the various values from [Alignment].
 *
 * Note: Glance's api for alignment is slightly different from Compose's, for example [Box] vs
 * [androidx.compose.foundation.layout.Box], the latter only takes a single alignment param that
 * contains both horizontal and vertical.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Config.TARGET_SDK])
class AlignmentTest : BaseRemoteComposeTest() {

    @Test
    fun glanceAlignment_toBoxLayoutEnum() {

        // horizontal
        assertEquals(BoxLayout.START, Alignment.Horizontal.Start.toBoxLayoutEnum())
        assertEquals(BoxLayout.CENTER, Alignment.Horizontal.CenterHorizontally.toBoxLayoutEnum())
        assertEquals(BoxLayout.END, Alignment.Horizontal.End.toBoxLayoutEnum())

        // Vertical
        assertEquals(BoxLayout.TOP, Alignment.Vertical.Top.toBoxLayoutEnum())
        assertEquals(BoxLayout.CENTER, Alignment.Vertical.CenterVertically.toBoxLayoutEnum())
        assertEquals(BoxLayout.BOTTOM, Alignment.Vertical.Bottom.toBoxLayoutEnum())
    }

    @Test
    fun glanceAlignment_toRowLayoutEnum() {

        // horizontal
        assertEquals(RowLayout.START, Alignment.Horizontal.Start.toRowLayoutEnum())
        assertEquals(RowLayout.CENTER, Alignment.Horizontal.CenterHorizontally.toRowLayoutEnum())
        assertEquals(RowLayout.END, Alignment.Horizontal.End.toRowLayoutEnum())

        // Vertical
        assertEquals(RowLayout.TOP, Alignment.Vertical.Top.toRowLayoutEnum())
        assertEquals(RowLayout.CENTER, Alignment.Vertical.CenterVertically.toRowLayoutEnum())
        assertEquals(RowLayout.BOTTOM, Alignment.Vertical.Bottom.toRowLayoutEnum())
    }

    @Test
    fun glanceAlignment_toColumnLayoutEnum() {

        // horizontal
        assertEquals(ColumnLayout.START, Alignment.Horizontal.Start.toColumnLayoutEnum())
        assertEquals(
            ColumnLayout.CENTER,
            Alignment.Horizontal.CenterHorizontally.toColumnLayoutEnum(),
        )
        assertEquals(ColumnLayout.END, Alignment.Horizontal.End.toColumnLayoutEnum())

        // Vertical
        assertEquals(ColumnLayout.TOP, Alignment.Vertical.Top.toColumnLayoutEnum())
        assertEquals(ColumnLayout.CENTER, Alignment.Vertical.CenterVertically.toColumnLayoutEnum())
        assertEquals(ColumnLayout.BOTTOM, Alignment.Vertical.Bottom.toColumnLayoutEnum())
    }
}

/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core.selection

import androidx.test.filters.SmallTest
import androidx.ui.core.LayoutCoordinates
import androidx.ui.unit.PxPosition
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@SmallTest
@RunWith(JUnit4::class)
class SelectionRegistrarImplTest {
    @Test
    fun subscribe() {
        val handler1: Selectable = mock()
        val handler2: Selectable = mock()
        val selectionRegistrar = SelectionRegistrarImpl()

        val id1 = selectionRegistrar.subscribe(handler1)
        val id2 = selectionRegistrar.subscribe(handler2)

        assertThat(id1).isEqualTo(handler1)
        assertThat(id2).isEqualTo(handler2)
        assertThat(selectionRegistrar.selectables.size).isEqualTo(2)
    }

    @Test
    fun unsubscribe() {
        val handler1: Selectable = mock()
        val handler2: Selectable = mock()
        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler1)
        val id2 = selectionRegistrar.subscribe(handler2)

        selectionRegistrar.unsubscribe(id2)

        assertThat(selectionRegistrar.selectables.size).isEqualTo(1)
    }

    @Test
    fun sort() {
        // Setup.
        val handler0 = mock<Selectable>()
        val handler1 = mock<Selectable>()
        val handler2 = mock<Selectable>()
        val handler3 = mock<Selectable>()

        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val layoutCoordinates1 = mock<LayoutCoordinates>()
        val layoutCoordinates2 = mock<LayoutCoordinates>()
        val layoutCoordinates3 = mock<LayoutCoordinates>()

        whenever(handler0.getLayoutCoordinates()).thenReturn(layoutCoordinates0)
        whenever(handler1.getLayoutCoordinates()).thenReturn(layoutCoordinates1)
        whenever(handler2.getLayoutCoordinates()).thenReturn(layoutCoordinates2)
        whenever(handler3.getLayoutCoordinates()).thenReturn(layoutCoordinates3)

        // The order of the 4 handlers should be 1, 0, 3, 2.
        val relativeCoordinates0 = PxPosition(20f, 12f)
        val relativeCoordinates1 = PxPosition(5f, 12f)
        val relativeCoordinates2 = PxPosition(20f, 24f)
        val relativeCoordinates3 = PxPosition(5f, 24f)

        val containerLayoutCoordinates = mock<LayoutCoordinates>()
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates0, PxPosition.Origin))
            .thenReturn(relativeCoordinates0)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates1, PxPosition.Origin))
            .thenReturn(relativeCoordinates1)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates2, PxPosition.Origin))
            .thenReturn(relativeCoordinates2)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates3, PxPosition.Origin))
            .thenReturn(relativeCoordinates3)

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.subscribe(handler1)
        selectionRegistrar.subscribe(handler2)
        selectionRegistrar.subscribe(handler3)

        // Act.
        selectionRegistrar.sort(containerLayoutCoordinates)

        // Assert.
        assertThat(selectionRegistrar.selectables[0]).isEqualTo(handler1)
        assertThat(selectionRegistrar.selectables[1]).isEqualTo(handler0)
        assertThat(selectionRegistrar.selectables[2]).isEqualTo(handler3)
        assertThat(selectionRegistrar.selectables[3]).isEqualTo(handler2)
        assertThat(selectionRegistrar.sorted).isTrue()
    }

    @Test
    fun unsubscribe_after_sorting() {
        // Setup.
        val handler0 = mock<Selectable>()
        val handler1 = mock<Selectable>()
        val handler2 = mock<Selectable>()
        val handler3 = mock<Selectable>()

        val layoutCoordinates0 = mock<LayoutCoordinates>()
        val layoutCoordinates1 = mock<LayoutCoordinates>()
        val layoutCoordinates2 = mock<LayoutCoordinates>()
        val layoutCoordinates3 = mock<LayoutCoordinates>()

        whenever(handler0.getLayoutCoordinates()).thenReturn(layoutCoordinates0)
        whenever(handler1.getLayoutCoordinates()).thenReturn(layoutCoordinates1)
        whenever(handler2.getLayoutCoordinates()).thenReturn(layoutCoordinates2)
        whenever(handler3.getLayoutCoordinates()).thenReturn(layoutCoordinates3)

        // The order of the 4 handlers should be 1, 0, 3, 2.
        val relativeCoordinates0 = PxPosition(20f, 12f)
        val relativeCoordinates1 = PxPosition(5f, 12f)
        val relativeCoordinates2 = PxPosition(20f, 24f)
        val relativeCoordinates3 = PxPosition(5f, 24f)

        val containerLayoutCoordinates = mock<LayoutCoordinates>()
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates0, PxPosition.Origin))
            .thenReturn(relativeCoordinates0)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates1, PxPosition.Origin))
            .thenReturn(relativeCoordinates1)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates2, PxPosition.Origin))
            .thenReturn(relativeCoordinates2)
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates3, PxPosition.Origin))
            .thenReturn(relativeCoordinates3)

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.subscribe(handler1)
        selectionRegistrar.subscribe(handler2)
        selectionRegistrar.subscribe(handler3)

        selectionRegistrar.sort(containerLayoutCoordinates)

        // Act.
        selectionRegistrar.unsubscribe(handler0)

        // Assert.
        assertThat(selectionRegistrar.selectables[0]).isEqualTo(handler1)
        assertThat(selectionRegistrar.selectables[1]).isEqualTo(handler3)
        assertThat(selectionRegistrar.selectables[2]).isEqualTo(handler2)
    }

    @Test
    fun subscribe_after_sorting() {
        // Setup.
        val handler0 = mock<Selectable>()
        val layoutCoordinates0 = mock<LayoutCoordinates>()
        whenever(handler0.getLayoutCoordinates()).thenReturn(layoutCoordinates0)
        val containerLayoutCoordinates = mock<LayoutCoordinates>()
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates0, PxPosition.Origin))
            .thenReturn(PxPosition.Origin)

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.sort(containerLayoutCoordinates)
        assertThat(selectionRegistrar.sorted).isTrue()

        // Act.
        selectionRegistrar.subscribe(mock())

        // Assert.
        assertThat(selectionRegistrar.sorted).isFalse()
    }

    @Test
    fun layoutCoordinates_changed_after_sorting() {
        // Setup.
        val handler0 = mock<Selectable>()
        val layoutCoordinates0 = mock<LayoutCoordinates>()
        whenever(handler0.getLayoutCoordinates()).thenReturn(layoutCoordinates0)
        val containerLayoutCoordinates = mock<LayoutCoordinates>()
        whenever(containerLayoutCoordinates.childToLocal(layoutCoordinates0, PxPosition.Origin))
            .thenReturn(PxPosition.Origin)

        val selectionRegistrar = SelectionRegistrarImpl()
        selectionRegistrar.subscribe(handler0)
        selectionRegistrar.sort(containerLayoutCoordinates)
        assertThat(selectionRegistrar.sorted).isTrue()

        // Act.
        selectionRegistrar.onPositionChange()

        // Assert.
        assertThat(selectionRegistrar.sorted).isFalse()
    }
}

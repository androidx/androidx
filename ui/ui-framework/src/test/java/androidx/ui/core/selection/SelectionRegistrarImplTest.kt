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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
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
}
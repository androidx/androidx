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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout
import com.google.common.truth.Truth.assertThat

internal fun ContextMenuState.open(offset: Offset = Offset.Zero) {
    status = Status.Open(offset)
}

internal fun ContextMenuScope.testItem(
    label: String = "Item",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    item(
        label = label,
        modifier = modifier,
        onClick = onClick,
    )
}

internal fun assertThatContextMenuState(state: ContextMenuState): ContextMenuStateSubject =
    assertAbout(ContextMenuStateSubject.SUBJECT_FACTORY).that(state)!!

internal class ContextMenuStateSubject internal constructor(
    failureMetadata: FailureMetadata?,
    private val subject: ContextMenuState
) : Subject(failureMetadata, subject) {
    companion object {
        internal val SUBJECT_FACTORY: Factory<ContextMenuStateSubject?, ContextMenuState> =
            Factory { failureMetadata, subject ->
                ContextMenuStateSubject(failureMetadata, subject)
            }
    }

    fun statusIsOpen() {
        assertThat(subject.status).isInstanceOf(Status.Open::class.java)
    }

    fun statusIsClosed() {
        assertThat(subject.status).isInstanceOf(Status.Closed::class.java)
    }
}

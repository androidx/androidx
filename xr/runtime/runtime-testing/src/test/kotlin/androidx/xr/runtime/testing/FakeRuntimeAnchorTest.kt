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

package androidx.xr.runtime.testing

import androidx.xr.runtime.internal.Anchor
import androidx.xr.runtime.math.Pose
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeRuntimeAnchorTest {

    @Test
    fun constructor_anchorHolderNotNull_isAttached() {
        val underTest = FakeRuntimeAnchor(Pose(), FakeRuntimePlane())

        assertThat(underTest.isAttached).isTrue()
    }

    @Test
    fun constructor_anchorHolderNull_isNotAttached() {
        val underTest = FakeRuntimeAnchor(Pose())

        assertThat(underTest.isAttached).isFalse()
    }

    @Test
    fun persist_setsUuidToRandomValueAndPersistenceStateToPersisted() {
        val underTest = FakeRuntimeAnchor(Pose())
        check(underTest.uuid == null)
        check(underTest.persistenceState == Anchor.PersistenceState.NOT_PERSISTED)

        underTest.persist()

        assertThat(underTest.uuid).isNotNull()
        assertThat(underTest.persistenceState).isEqualTo(Anchor.PersistenceState.PERSISTED)
    }

    @Test
    fun detach_attachedBecomesFalse() {
        val underTest = FakeRuntimeAnchor(Pose(), FakeRuntimePlane())
        check(underTest.isAttached.equals(true))

        underTest.detach()

        assertThat(underTest.isAttached).isFalse()
    }
}

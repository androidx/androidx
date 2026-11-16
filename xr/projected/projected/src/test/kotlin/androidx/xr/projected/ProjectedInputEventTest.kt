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

package androidx.xr.projected

import androidx.xr.projected.ProjectedInputEvent.ProjectedInputAction
import androidx.xr.projected.experimental.ExperimentalProjectedApi
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalProjectedApi::class)
class ProjectedInputEventTest {

    @Test
    fun projectedInputAction_equals_distinctInstanceWithSameCode_isEqual() {
        val action = ProjectedInputAction(0)

        assertThat(action).isNotSameInstanceAs(ProjectedInputAction.TOGGLE_APP_CAMERA)
        assertThat(action).isEqualTo(ProjectedInputAction.TOGGLE_APP_CAMERA)
        assertThat(action.hashCode()).isEqualTo(ProjectedInputAction.TOGGLE_APP_CAMERA.hashCode())
    }

    @Test
    fun projectedInputAction_equals_differentCode_isNotEqual() {
        assertThat(ProjectedInputAction(99)).isNotEqualTo(ProjectedInputAction.TOGGLE_APP_CAMERA)
    }

    @Test
    fun projectedInputAction_toString_returnsConstantName() {
        assertThat(ProjectedInputAction.TOGGLE_APP_CAMERA.toString()).isEqualTo("TOGGLE_APP_CAMERA")
    }

    @Test
    fun projectedInputAction_toString_unknownCode_returnsUnknown() {
        assertThat(ProjectedInputAction(99).toString()).isEqualTo("UNKNOWN(99)")
    }

    @Test
    fun projectedInputEvent_equals_sameAction_isEqual() {
        val event = ProjectedInputEvent(ProjectedInputAction.TOGGLE_APP_CAMERA)
        val other = ProjectedInputEvent(ProjectedInputAction.TOGGLE_APP_CAMERA)

        assertThat(event).isEqualTo(other)
        assertThat(event.hashCode()).isEqualTo(other.hashCode())
    }

    @Test
    fun projectedInputEvent_toString_includesActionName() {
        assertThat(ProjectedInputEvent(ProjectedInputAction.TOGGLE_APP_CAMERA).toString())
            .isEqualTo("ProjectedInputEvent(inputAction=TOGGLE_APP_CAMERA)")
    }
}

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

package androidx.glance.wear

import android.content.ComponentName
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ActiveWearWidgetHandleTest {

    private val provider1 = ComponentName("pkg1", "cls1")
    private val provider2 = ComponentName("pkg2", "cls2")

    @Test
    fun equals_sameInstance() {
        val id = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id).isEqualTo(id)
    }

    @Test
    fun equals_SameValues() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun equals_differentProvider() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider2, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun equals_differentInstanceId() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 123, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun equals_differentContainerType() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_SMALL)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun equals_differentType() {
        val id = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id).isNotEqualTo(ContainerInfo.CONTAINER_TYPE_LARGE)
    }

    @Test
    fun equals_null() {
        val id = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id).isNotEqualTo(null)
    }

    @Test
    fun hashCode_sameForEqualObjects() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentForDifferentProvider() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider2, 17, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentForDifferentInstanceId() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 123, ContainerInfo.CONTAINER_TYPE_LARGE)

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentForDifferentContainerType() {
        val id1 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_LARGE)
        val id2 = ActiveWearWidgetHandle(provider1, 17, ContainerInfo.CONTAINER_TYPE_SMALL)

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }
}

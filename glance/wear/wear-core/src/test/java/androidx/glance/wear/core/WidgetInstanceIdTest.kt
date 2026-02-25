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

package androidx.glance.wear.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [org.robolectric.annotation.Config.TARGET_SDK])
class WidgetInstanceIdTest {

    @Test
    fun equals_sameInstance_isEqual() {
        val id = WidgetInstanceId("ns", 17)

        assertThat(id).isEqualTo(id)
    }

    @Test
    fun equals_sameValues_isEqual() {
        val value = 17
        val namespace = "ns"
        val id1 = WidgetInstanceId(namespace, value)
        val id2 = WidgetInstanceId(namespace, value)

        assertThat(id1).isEqualTo(id2)
    }

    @Test
    fun equals_differentIds_isNotEqual() {
        val namespace = "ns"
        val id1 = WidgetInstanceId(namespace, 17)
        val id2 = WidgetInstanceId(namespace, 123)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun equals_differentNamespace_isNotEqual() {
        val id1 = WidgetInstanceId("ns1", 17)
        val id2 = WidgetInstanceId("ns2", 17)

        assertThat(id1).isNotEqualTo(id2)
    }

    @Test
    fun equals_differentType_isNotEqual() {
        val id = WidgetInstanceId("ns", 17)

        assertThat(id).isNotEqualTo(17)
    }

    @Test
    fun equals_null_isNotEqual() {
        val id = WidgetInstanceId("ns", 17)

        assertThat(id).isNotEqualTo(null)
    }

    @Test
    fun hashCode_equalObjects_isEqual() {
        val namespace = "ns"
        val id1 = WidgetInstanceId(namespace, 17)
        val id2 = WidgetInstanceId(namespace, 17)

        assertThat(id1.hashCode()).isEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentIds_isNotEqual() {
        val namespace = "ns"
        val id1 = WidgetInstanceId(namespace, 17)
        val id2 = WidgetInstanceId(namespace, 123)

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun hashCode_differentNamespaces_isNotEqual() {
        val id1 = WidgetInstanceId("ns1", 17)
        val id2 = WidgetInstanceId("ns2", 17)

        assertThat(id1.hashCode()).isNotEqualTo(id2.hashCode())
    }

    @Test
    fun flattenToStringIsStable() {
        val namespace = "ns"
        val id1 = WidgetInstanceId(namespace, 17)
        val id2 = WidgetInstanceId(namespace, 17)

        assertThat(id1).isEqualTo(id2)
        assertThat(id1.flattenToString()).isEqualTo(id2.flattenToString())
    }
}

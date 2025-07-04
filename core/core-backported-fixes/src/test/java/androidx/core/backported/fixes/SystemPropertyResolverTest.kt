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

package androidx.core.backported.fixes

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowBuild
import org.robolectric.shadows.ShadowSystemProperties

/** Unit tests for [SystemPropertyResolver]. */
@RunWith(RobolectricTestRunner::class)
class SystemPropertyResolverTest {

    @Test
    fun aliases_null() {
        ShadowSystemProperties.override(ALIAS_BITSET_PROP_NAME, null)
        ShadowBuild.reset()
        val resolver = SystemPropertyResolver()
        assertThat(resolver.aliases).isEmpty()
    }

    @Test
    fun aliases_empty() {
        ShadowSystemProperties.override(ALIAS_BITSET_PROP_NAME, "")
        ShadowBuild.reset()
        val resolver = SystemPropertyResolver()
        assertThat(resolver.aliases).isEmpty()
    }

    @Test
    fun aliases_1_3() {
        ShadowSystemProperties.override(ALIAS_BITSET_PROP_NAME, "10")
        ShadowBuild.reset()
        val resolver = SystemPropertyResolver()
        assertThat(resolver.aliases).containsExactly(1, 3)
    }

    @Test
    fun aliases_1023() {
        ShadowSystemProperties.override(
            ALIAS_BITSET_PROP_NAME,
            // java.lang.Long.parseLong("-9223372036854775808").toHexString()) = 8000000000000000
            "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,-9223372036854775808",
        )
        ShadowBuild.reset()
        val resolver = SystemPropertyResolver()
        assertThat(resolver.aliases).containsExactly(1023)
    }

    fun aliases_partial_bad() {
        ShadowSystemProperties.override(ALIAS_BITSET_PROP_NAME, "4,bad,2")
        ShadowBuild.reset()
        val resolver = SystemPropertyResolver()
        assertThat(resolver.aliases).containsExactly(3)
    }
}

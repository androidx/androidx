/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.build

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class KmpPlatformsTest {

    @Test
    fun withAnEmptyFlag_itReturnsTheDefaultValue() {
        assertThat(KmpFlagParser.parse("")).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withANullFlag_itReturnsTheDefaultValue() {
        assertThat(KmpFlagParser.parse(null)).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withASingleDefaultPlatform_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("+jvm")).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withNoPlatforms_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("-jvm,-desktop")).isEqualTo(emptySet<KmpPlatform>())
    }

    @Test
    fun withASingleNonDefaultPlatform_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("+js")).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.JS, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withAMultiplePlatforms_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("+js,+mac")).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.JS, KmpPlatform.MAC, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withNegativeFlags_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("-jvm,+mac")).isEqualTo(
            setOf(KmpPlatform.MAC, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withTheNativeFlag_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("+native")).isEqualTo(
            setOf(KmpPlatform.JVM, KmpPlatform.MAC, KmpPlatform.LINUX, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withMultipleFlagsIncludingTheNativeFlag_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("-jvm,+native,+js")).isEqualTo(
            setOf(KmpPlatform.JS, KmpPlatform.MAC, KmpPlatform.LINUX, KmpPlatform.DESKTOP)
        )
    }

    @Test
    fun withRedundentFlags_itParsesTheFlagCorrectly() {
        assertThat(KmpFlagParser.parse("-jvm,+native,+linux,+mac,+linux")).isEqualTo(
            setOf(KmpPlatform.MAC, KmpPlatform.LINUX, KmpPlatform.DESKTOP)
        )
    }
}
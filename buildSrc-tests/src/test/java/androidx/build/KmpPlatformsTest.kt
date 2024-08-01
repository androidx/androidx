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
        assertThat(parseTargetPlatformsFlag(""))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS
                )
            )
    }

    @Test
    fun withANullFlag_itReturnsTheDefaultValue() {
        assertThat(parseTargetPlatformsFlag(null))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS,
                )
            )
    }

    @Test
    fun withASingleDefaultPlatform_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("+jvm"))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS,
                )
            )
    }

    @Test
    fun withNoPlatforms_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("-jvm,-desktop,-native,-wasm,-js"))
            .isEqualTo(emptySet<PlatformGroup>())
    }

    @Test
    fun withASingleNonDefaultPlatform_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("+js"))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.JS,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS,
                )
            )
    }

    @Test
    fun withAMultiplePlatforms_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("+js,+mac"))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.JS,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS,
                )
            )
    }

    @Test
    fun withNegativeFlags_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("-jvm,+mac,-wasm,-js"))
            .isEqualTo(
                setOf(
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                )
            )
    }

    @Test
    fun withTheNativeFlag_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("+native"))
            .isEqualTo(
                setOf(
                    PlatformGroup.JVM,
                    PlatformGroup.WASM,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE,
                    PlatformGroup.JS
                )
            )
    }

    @Test
    fun withMultipleFlagsIncludingTheNativeFlag_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("-jvm,+native,+js,-wasm"))
            .isEqualTo(
                setOf(
                    PlatformGroup.JS,
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE
                )
            )
    }

    @Test
    fun withRedundentFlags_itParsesTheFlagCorrectly() {
        assertThat(parseTargetPlatformsFlag("-wasm,-jvm,+native,+linux,+mac,+linux,-wasm,-js"))
            .isEqualTo(
                setOf(
                    PlatformGroup.MAC,
                    PlatformGroup.WINDOWS,
                    PlatformGroup.LINUX,
                    PlatformGroup.DESKTOP,
                    PlatformGroup.ANDROID_NATIVE
                )
            )
    }
}

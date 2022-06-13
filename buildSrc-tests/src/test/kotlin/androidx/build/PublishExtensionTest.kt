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

class PublishExtensionTest {

    @Test
    fun publishPlatformsDefault() {
        val extension = PublishExtension()
        assertThat(extension.publishPlatforms).isEqualTo(emptyList<String>())
    }

    @Test
    fun publishPlatformsWithJvmAndJs() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_ONLY
            js = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.publishPlatforms).isEqualTo(listOf("jvm", "js"))
    }

    @Test
    fun publishPlatformsWithJvmAndJsBuildOnly() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_ONLY
            js = Publish.NONE
        }
        assertThat(extension.publishPlatforms).isEqualTo(listOf("jvm"))
    }

    @Test
    fun publishExtensionMultiPlatformEnabledDefault() {
        val extension = PublishExtension()
        assertThat(extension.shouldEnableMultiplatform()).isFalse()
    }

    @Test
    fun publishExtensionMultiPlatformEnabledWhenNotBuilding() {
        val extension = PublishExtension().apply {
            jvm = Publish.UNSET
            js = Publish.UNSET
            mac = Publish.UNSET
            linux = Publish.UNSET
        }
        assertThat(extension.shouldEnableMultiplatform()).isFalse()
    }

    @Test
    fun publishExtensionMultiPlatformEnabledWhenBuildingAtLeastOnePlatform() {
        val extension = PublishExtension().apply {
            jvm = Publish.NONE
        }
        assertThat(extension.shouldEnableMultiplatform()).isTrue()
    }

    @Test
    fun publishExtensionMultiPlatformEnabledWhenPublishing() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_ONLY
            js = Publish.SNAPSHOT_ONLY
            linux = Publish.SNAPSHOT_ONLY
            mac = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.shouldEnableMultiplatform()).isTrue()
    }

    @Test
    fun shouldPublish_whenOnlyAndroid() {
        val extension = PublishExtension().apply {
            android = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.shouldPublishAny()).isTrue()
    }

    @Test
    fun shouldPublish_whenOnlyNonAndroid() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.shouldPublishAny()).isTrue()
    }

    @Test
    fun shouldPublish_whenNotPublishing() {
        val extension = PublishExtension().apply {
            android = Publish.NONE
        }
        assertThat(extension.shouldPublishAny()).isFalse()
    }

    @Test
    fun shouldRelease_whenOnlyAndroid() {
        val extension = PublishExtension().apply {
            android = Publish.SNAPSHOT_AND_RELEASE
        }
        assertThat(extension.shouldReleaseAny()).isTrue()
    }

    @Test
    fun shouldRelease_whenOnlyNonAndroid() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_AND_RELEASE
        }
        assertThat(extension.shouldReleaseAny()).isTrue()
    }

    @Test
    fun shouldRelease_whenNotReleasing() {
        val extension = PublishExtension().apply {
            android = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.shouldReleaseAny()).isFalse()
    }

    @Test
    fun isPublishConfigured_whenNotConfigured() {
        val extension = PublishExtension()
        assertThat(extension.isPublishConfigured()).isFalse()
    }

    @Test
    fun isPublishConfigured_whenOnlyAndroid() {
        val extension = PublishExtension().apply {
            android = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.isPublishConfigured()).isTrue()
    }

    @Test
    fun isPublishConfigured_whenOnlyNonAndroid() {
        val extension = PublishExtension().apply {
            jvm = Publish.SNAPSHOT_ONLY
        }
        assertThat(extension.isPublishConfigured()).isTrue()
    }

    @Test
    fun isPublishConfigured_whenBuildOnly() {
        val extension = PublishExtension().apply {
            android = Publish.NONE
        }
        assertThat(extension.isPublishConfigured()).isTrue()
    }
}
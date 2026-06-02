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

package androidx.compose.remote.testing

import androidx.compose.remote.core.Limits
import org.junit.rules.ExternalResource

/**
 * A [org.junit.rules.TestRule] that allows overriding [Limits] for the duration of a test and
 * restores them to their original values afterwards.
 */
class LimitsRule : ExternalResource() {
    private var maxOpCount: Int? = null
    private var maxImageDimension: Int? = null
    private var maxBitmapMemory: Int? = null
    private var maxFps: Int? = null
    private var defaultMaxFps: Int? = null
    private var enableImageUrls: Boolean? = null
    private var enableImageFiles: Boolean? = null

    private var originalMaxOpCount: Int = 0
    private var originalMaxImageDimension: Int = 0
    private var originalMaxBitmapMemory: Int = 0
    private var originalMaxFps: Int = 0
    private var originalDefaultMaxFps: Int = 0
    private var originalEnableImageUrls: Boolean = false
    private var originalEnableImageFiles: Boolean = false

    fun maxOpCount(value: Int) = apply { this.maxOpCount = value }

    fun maxImageDimension(value: Int) = apply { this.maxImageDimension = value }

    fun maxBitmapMemory(value: Int) = apply { this.maxBitmapMemory = value }

    fun maxFps(value: Int) = apply { this.maxFps = value }

    fun defaultMaxFps(value: Int) = apply { this.defaultMaxFps = value }

    fun enableImageUrls(value: Boolean) = apply { this.enableImageUrls = value }

    fun enableImageFiles(value: Boolean) = apply { this.enableImageFiles = value }

    fun setMaxOpCount(value: Int) {
        this.maxOpCount = value
        Limits.MAX_OP_COUNT = value
    }

    fun setMaxImageDimension(value: Int) {
        this.maxImageDimension = value
        Limits.MAX_IMAGE_DIMENSION = value
    }

    fun setMaxBitmapMemory(value: Int) {
        this.maxBitmapMemory = value
        Limits.MAX_BITMAP_MEMORY = value
    }

    fun setMaxFps(value: Int) {
        this.maxFps = value
        Limits.MAX_FPS = value
    }

    fun setDefaultMaxFps(value: Int) {
        this.defaultMaxFps = value
        Limits.DEFAULT_MAX_FPS = value
    }

    fun setEnableImageUrls(value: Boolean) {
        this.enableImageUrls = value
        Limits.ENABLE_IMAGE_URLS = value
    }

    fun setEnableImageFiles(value: Boolean) {
        this.enableImageFiles = value
        Limits.ENABLE_IMAGE_FILES = value
    }

    override fun before() {
        originalMaxOpCount = Limits.MAX_OP_COUNT
        originalMaxImageDimension = Limits.MAX_IMAGE_DIMENSION
        originalMaxBitmapMemory = Limits.MAX_BITMAP_MEMORY
        originalMaxFps = Limits.MAX_FPS
        originalDefaultMaxFps = Limits.DEFAULT_MAX_FPS
        originalEnableImageUrls = Limits.ENABLE_IMAGE_URLS
        originalEnableImageFiles = Limits.ENABLE_IMAGE_FILES

        maxOpCount?.let { Limits.MAX_OP_COUNT = it }
        maxImageDimension?.let { Limits.MAX_IMAGE_DIMENSION = it }
        maxBitmapMemory?.let { Limits.MAX_BITMAP_MEMORY = it }
        maxFps?.let { Limits.MAX_FPS = it }
        defaultMaxFps?.let { Limits.DEFAULT_MAX_FPS = it }
        enableImageUrls?.let { Limits.ENABLE_IMAGE_URLS = it }
        enableImageFiles?.let { Limits.ENABLE_IMAGE_FILES = it }
    }

    override fun after() {
        Limits.MAX_OP_COUNT = originalMaxOpCount
        Limits.MAX_IMAGE_DIMENSION = originalMaxImageDimension
        Limits.MAX_BITMAP_MEMORY = originalMaxBitmapMemory
        Limits.MAX_FPS = originalMaxFps
        Limits.DEFAULT_MAX_FPS = originalDefaultMaxFps
        Limits.ENABLE_IMAGE_URLS = originalEnableImageUrls
        Limits.ENABLE_IMAGE_FILES = originalEnableImageFiles
    }
}

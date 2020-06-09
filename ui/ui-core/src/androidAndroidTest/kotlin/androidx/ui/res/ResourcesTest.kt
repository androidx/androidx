/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.res

import android.util.LruCache
import androidx.compose.Providers
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.ContextAmbient
import androidx.ui.core.test.R
import androidx.ui.graphics.ImageAsset
import androidx.ui.graphics.asAndroidBitmap
import androidx.ui.graphics.imageFromResource
import androidx.ui.test.createComposeRule
import androidx.ui.test.runOnIdleCompose
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.concurrent.Executor

@RunWith(JUnit4::class)
@SmallTest
class ResourcesTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    class PendingExecutor : Executor {
        var runnable: Runnable? = null

        override fun execute(r: Runnable) {
            runnable = r
        }

        fun runNow() {
            runnable?.run()
            runnable = null
        }
    }

    @Test
    fun asyncLoadingTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resource = context.resources
        val pendingExecutor = PendingExecutor()
        val cacheLock = Any()
        val requestCache = mutableMapOf<String, MutableList<DeferredResource<*>>>()
        val resourceCache = LruCache<String, Any>(1)

        val loadedImage = imageFromResource(resource, R.drawable.loaded_image)
        val pendingImage = imageFromResource(resource, R.drawable.pending_image)
        val failedImage = imageFromResource(resource, R.drawable.failed_image)

        var uiThreadWork: (() -> Unit)? = null
        var res: DeferredResource<ImageAsset>? = null

        composeTestRule.setContent {
            Providers(ContextAmbient provides context) {
                res = loadResourceInternal(
                    key = "random key string",
                    pendingResource = pendingImage,
                    failedResource = failedImage,
                    executor = pendingExecutor,
                    uiThreadHandler = { uiThreadWork = it },
                    cacheLock = cacheLock,
                    requestCache = requestCache,
                    resourceCache = resourceCache,
                    loader = { imageFromResource(resource, R.drawable.loaded_image) }
                )
            }
        }

        runOnIdleCompose {
            assertThat(pendingExecutor.runnable).isNotNull()
            assertThat(res!!.resource).isInstanceOf(PendingResource::class.java)
            assertThat(res!!.resource.resource).isNotNull()
            assertThat(res!!.resource.resource!!.asAndroidBitmap().sameAs(pendingImage
                .asAndroidBitmap()))
        }

        runOnIdleCompose {
            pendingExecutor.runNow() // load the resource
            assertThat(uiThreadWork).isNotNull()
            // update state object so that recompose is expected to be triggered.
            uiThreadWork?.invoke()
        }

        runOnIdleCompose {
            assertThat(pendingExecutor.runnable).isNull()
            assertThat(res!!.resource).isInstanceOf(LoadedResource::class.java)
            assertThat(res!!.resource.resource).isNotNull()
            assertThat(res!!.resource.resource!!
                .asAndroidBitmap()
                .sameAs(loadedImage.asAndroidBitmap()))
                .isTrue()
        }
    }

    @Test
    fun asyncLoadingFailTest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resource = context.resources
        val pendingExecutor = PendingExecutor()
        val cacheLock = Any()
        val requestCache = mutableMapOf<String, MutableList<DeferredResource<*>>>()
        val resourceCache = LruCache<String, Any>(1)

        val pendingImage = imageFromResource(resource, R.drawable.pending_image)
        val failedImage = imageFromResource(resource, R.drawable.failed_image)

        var uiThreadWork: (() -> Unit)? = null
        var res: DeferredResource<ImageAsset>? = null

        composeTestRule.setContent {
            Providers(ContextAmbient provides context) {
                res = loadResourceInternal(
                    key = "random key string",
                    pendingResource = pendingImage,
                    failedResource = failedImage,
                    executor = pendingExecutor,
                    uiThreadHandler = { uiThreadWork = it },
                    cacheLock = cacheLock,
                    requestCache = requestCache,
                    resourceCache = resourceCache,
                    loader = { throw RuntimeException("Resource Load Failed") }
                )
            }
        }

        runOnIdleCompose {
            assertThat(pendingExecutor.runnable).isNotNull()
            assertThat(res!!.resource).isInstanceOf(PendingResource::class.java)
            assertThat(res!!.resource.resource).isNotNull()
            assertThat(res!!.resource.resource!!
                .asAndroidBitmap()
                .sameAs(pendingImage.asAndroidBitmap()))
                .isTrue()
        }

        runOnIdleCompose {
            pendingExecutor.runNow() // load the resource
            assertThat(uiThreadWork).isNotNull()
            // update state object so that recompose is expected to be triggered.
            uiThreadWork?.invoke()
        }

        runOnIdleCompose {
            assertThat(pendingExecutor.runnable).isNull()
            assertThat(res!!.resource).isInstanceOf(FailedResource::class.java)
            assertThat(res!!.resource.resource).isNotNull()
            assertThat(res!!.resource.resource!!
                .asAndroidBitmap()
                .sameAs(failedImage.asAndroidBitmap()))
                .isTrue()
        }
    }
}
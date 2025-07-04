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

package androidx.wear.tiles.renderer.test

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.wear.protolayout.proto.LayoutElementProto
import androidx.wear.protolayout.proto.ResourceProto
import androidx.wear.protolayout.renderer.ProtoLayoutVisibilityState
import androidx.wear.protolayout.renderer.impl.ProtoLayoutViewInstance
import com.google.common.util.concurrent.MoreExecutors
import java.util.concurrent.ExecutionException

class GoldenTestActivity : Activity() {
    protected override fun onCreate(savedInstanceState: Bundle?) {
        val layoutPayload = intent.extras!!.getByteArray(EXTRA_LAYOUT_KEY)
        val layoutProto = LayoutElementProto.Layout.parseFrom(layoutPayload!!)

        val root =
            FrameLayout(applicationContext).apply {
                setBackgroundColor(Color.BLACK)
                layoutParams = FrameLayout.LayoutParams(SCREEN_SIZE, SCREEN_SIZE)
            }

        val mainExecutor = MoreExecutors.newDirectExecutorService()
        val instance =
            ProtoLayoutViewInstance(
                ProtoLayoutViewInstance.Config.Builder(
                        applicationContext,
                        mainExecutor,
                        mainExecutor,
                        "androidx.wear.tiles.extra.CLICKABLE_ID",
                    )
                    .build()
            )
        instance.setLayoutVisibility(ProtoLayoutVisibilityState.VISIBILITY_STATE_FULLY_VISIBLE)

        try {
            instance.renderAndAttach(layoutProto, generateResourcesProto(), root).get()
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException(e)
        }

        // Simulate what the thing outside the renderer should do. Center the contents.
        val layoutParams = root.getChildAt(0).layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = Gravity.CENTER

        // Set the activity to be full screen so when we crop the Bitmap we don't get time bar etc.
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        setContentView(root, ViewGroup.LayoutParams(SCREEN_SIZE, SCREEN_SIZE))

        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())

        super.onCreate(savedInstanceState)
    }

    companion object {
        private const val SCREEN_SIZE = 390
        const val EXTRA_LAYOUT_KEY = "layout"

        private fun generateResourcesProto(): ResourceProto.Resources =
            ResourceProto.Resources.newBuilder()
                .putIdToImage(
                    "android",
                    ResourceProto.ImageResource.newBuilder()
                        .setAndroidResourceByResId(
                            ResourceProto.AndroidImageResourceByResId.newBuilder()
                                .setResourceId(R.drawable.android_24dp)
                        )
                        .build(),
                )
                .putIdToImage(
                    "android_withbg_120dp",
                    ResourceProto.ImageResource.newBuilder()
                        .setAndroidResourceByResId(
                            ResourceProto.AndroidImageResourceByResId.newBuilder()
                                .setResourceId(R.drawable.android_withbg_120dp)
                        )
                        .build(),
                )
                .putIdToImage(
                    "broken_image",
                    ResourceProto.ImageResource.newBuilder()
                        .setAndroidResourceByResId(
                            ResourceProto.AndroidImageResourceByResId.newBuilder()
                                .setResourceId(R.drawable.broken_drawable)
                        )
                        .build(),
                )
                .putIdToImage(
                    "missing_image",
                    ResourceProto.ImageResource.newBuilder()
                        .setAndroidResourceByResId(
                            ResourceProto.AndroidImageResourceByResId.newBuilder().setResourceId(-1)
                        )
                        .build(),
                )
                .build()
    }
}

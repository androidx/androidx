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

package androidx.graphics.lowlatency

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup

class FrontBufferedRendererTestActivity : Activity() {

    private lateinit var mSurfaceView: TestSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val surfaceView = TestSurfaceView(this).also { mSurfaceView = it }
        setContentView(surfaceView, ViewGroup.LayoutParams(WIDTH, HEIGHT))
    }

    fun getSurfaceView(): TestSurfaceView = mSurfaceView

    companion object {
        const val WIDTH = 100
        const val HEIGHT = 100
    }

    class TestSurfaceView(context: Context) : SurfaceView(context) {

        private var mHolderWrapper: HolderWrapper? = null

        override fun getHolder(): SurfaceHolder {
            var wrapper = mHolderWrapper
            if (wrapper == null) {
                wrapper = HolderWrapper(super.getHolder()).also { mHolderWrapper = it }
            }
            return wrapper
        }

        fun getCallbackCount(): Int = mHolderWrapper?.mCallbacks?.size ?: 0

        class HolderWrapper(val wrapped: SurfaceHolder) : SurfaceHolder by wrapped {

            val mCallbacks = ArrayList<SurfaceHolder.Callback>()

            override fun addCallback(callback: SurfaceHolder.Callback) {
                if (!mCallbacks.contains(callback)) {
                    mCallbacks.add(callback)
                    wrapped.addCallback(callback)
                }
            }

            override fun removeCallback(callback: SurfaceHolder.Callback) {
                if (mCallbacks.contains(callback)) {
                    mCallbacks.remove(callback)
                    wrapped.removeCallback(callback)
                }
            }
        }
    }
}
/*
 * Copyright 2023 The Android Open Source Project
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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class LowLatencyActivity : Activity() {

    private lateinit var mLowLatencyCanvasView: LowLatencyCanvasView
    private var mOnDestroyCallback: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        super.onCreate(savedInstanceState)
        mLowLatencyCanvasView = LowLatencyCanvasView(this)
    }

    fun attachLowLatencyView() {
        setContentView(mLowLatencyCanvasView)
    }

    fun setOnDestroyCallback(callback: () -> Unit) {
        mOnDestroyCallback = callback
    }

    fun getLowLatencyCanvasView() = mLowLatencyCanvasView

    override fun onDestroy() {
        super.onDestroy()
        mOnDestroyCallback?.invoke()
    }
}

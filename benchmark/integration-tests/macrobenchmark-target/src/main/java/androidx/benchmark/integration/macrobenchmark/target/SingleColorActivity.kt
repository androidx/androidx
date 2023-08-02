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

package androidx.benchmark.integration.macrobenchmark.target

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout

class SingleColorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.single_color)
        if (Build.VERSION.SDK_INT >= 30) {
            Api30Fullscreen.hideStatusBar(getWindow())
        }

        val bg = findViewById<ConstraintLayout>(R.id.color_background)
        val color = intent.getIntExtra(BG_COLOR, Color.RED)
        bg.setBackgroundColor(color)
    }

    companion object {
        const val BG_COLOR = "bg_color"
    }

    @RequiresApi(30)
    private object Api30Fullscreen {
        @JvmStatic
        @DoNotInline
        fun hideStatusBar(window: Window) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }
    }
}

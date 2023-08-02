/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.demo.embedding

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.window.demo.databinding.ActivitySplitActivityPlaceholderLayoutBinding

open class SplitActivityPlaceholder : AppCompatActivity() {

    lateinit var viewBinding: ActivitySplitActivityPlaceholderLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivitySplitActivityPlaceholderLayoutBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        viewBinding.rootSplitActivityLayout.setBackgroundColor(Color.parseColor("#eeeeee"))
    }
}

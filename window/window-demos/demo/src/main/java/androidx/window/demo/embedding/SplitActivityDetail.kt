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

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.window.demo.common.EdgeToEdgeActivity
import androidx.window.demo.databinding.ActivitySplitActivityListDetailLayoutBinding

open class SplitActivityDetail : EdgeToEdgeActivity() {

    private lateinit var viewBinding: ActivitySplitActivityListDetailLayoutBinding
    private lateinit var itemDetailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySplitActivityListDetailLayoutBinding.inflate(layoutInflater)
        viewBinding.rootSplitActivityLayout.setBackgroundColor(Color.parseColor("#fff3e0"))
        setContentView(viewBinding.root)
        itemDetailTextView = viewBinding.itemDetailText

        itemDetailTextView.text = intent.getStringExtra(EXTRA_SELECTED_ITEM)

        window.decorView.setOnFocusChangeListener { _, focus ->
            itemDetailTextView.text = "${itemDetailTextView.text} focus=$focus"
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        itemDetailTextView.text = intent.getStringExtra(EXTRA_SELECTED_ITEM)
    }

    companion object {
        const val EXTRA_SELECTED_ITEM = "selected_item"
    }
}

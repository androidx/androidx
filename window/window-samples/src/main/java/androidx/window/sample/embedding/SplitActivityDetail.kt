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

package androidx.window.sample.embedding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.window.sample.R

open class SplitActivityDetail : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_activity_list_detail_layout)
        findViewById<View>(R.id.root_split_activity_layout)
            .setBackgroundColor(Color.parseColor("#fff3e0"))

        findViewById<TextView>(R.id.item_detail_text)
            .setText(intent.getStringExtra(EXTRA_SELECTED_ITEM))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        findViewById<TextView>(R.id.item_detail_text)
            .setText(intent?.getStringExtra(EXTRA_SELECTED_ITEM))
    }

    companion object {
        const val EXTRA_SELECTED_ITEM = "selected_item"
    }
}
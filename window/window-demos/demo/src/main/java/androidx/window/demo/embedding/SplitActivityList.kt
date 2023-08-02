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
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.demo.R
import androidx.window.demo.embedding.SplitActivityDetail.Companion.EXTRA_SELECTED_ITEM
import androidx.window.embedding.SplitController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class SplitActivityList : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_activity_list_layout)
        findViewById<View>(R.id.root_split_activity_layout)
            .setBackgroundColor(Color.parseColor("#e0f7fa"))
        val splitController = SplitController.getInstance(this)

        lifecycleScope.launch {
            // The block passed to repeatOnLifecycle is executed when the lifecycle
            // is at least STARTED and is cancelled when the lifecycle is STOPPED.
            // It automatically restarts the block when the lifecycle is STARTED again.
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                splitController.splitInfoList(this@SplitActivityList)
                    .collect { newSplitInfos ->
                        withContext(Dispatchers.Main) {
                            findViewById<View>(R.id.infoButton).visibility =
                                if (newSplitInfos.isEmpty()) View.VISIBLE else View.GONE
                        }
                    }
            }
        }
    }

    open fun onItemClick(view: View) {
        val text = (view as TextView).text ?: throw IllegalArgumentException()
        val startIntent = Intent(this, SplitActivityDetail::class.java)
        startIntent.putExtra(EXTRA_SELECTED_ITEM, text)
        startActivity(startIntent)
    }
}

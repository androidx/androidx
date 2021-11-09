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
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.window.core.ExperimentalWindowApi
import androidx.window.embedding.SplitController
import androidx.window.embedding.SplitInfo
import androidx.window.sample.R
import androidx.window.sample.embedding.SplitActivityDetail.Companion.EXTRA_SELECTED_ITEM

@OptIn(ExperimentalWindowApi::class)
open class SplitActivityList : AppCompatActivity() {
    lateinit var splitController: SplitController
    val splitChangeListener = SplitStateChangeListener()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_split_activity_list_layout)
        findViewById<View>(R.id.root_split_activity_layout)
            .setBackgroundColor(Color.parseColor("#e0f7fa"))

        splitController = SplitController.getInstance()
    }

    open fun onItemClick(view: View) {
        val text = (view as TextView).text ?: throw IllegalArgumentException()
        val startIntent = Intent(this, SplitActivityDetail::class.java)
        startIntent.putExtra(EXTRA_SELECTED_ITEM, text)
        startActivity(startIntent)
    }

    override fun onStart() {
        super.onStart()
        splitController.addSplitListener(
            this,
            ContextCompat.getMainExecutor(this),
            splitChangeListener
        )
    }

    override fun onStop() {
        super.onStop()
        splitController.removeSplitListener(
            splitChangeListener
        )
    }

    inner class SplitStateChangeListener : Consumer<List<SplitInfo>> {
        override fun accept(newSplitInfos: List<SplitInfo>) {
            findViewById<View>(R.id.infoButton).visibility =
                if (newSplitInfos.isEmpty()) View.VISIBLE else View.GONE
        }
    }
}
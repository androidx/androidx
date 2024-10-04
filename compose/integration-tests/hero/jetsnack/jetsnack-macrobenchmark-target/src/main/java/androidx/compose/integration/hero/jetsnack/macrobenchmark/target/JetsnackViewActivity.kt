/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.integration.hero.jetsnack.macrobenchmark.target

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.integration.hero.jetsnack.implementation.SnackRepo
import androidx.compose.integration.hero.jetsnack.implementation.views.FeedAdapter
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class JetsnackViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_jetsnack_view)
        val feedAdapter = FeedAdapter(SnackRepo.getSnacks())

        val recycler = findViewById<RecyclerView>(R.id.snackFeedRecyclerView)
        with(recycler) {
            layoutManager = LinearLayoutManager(context!!)
            adapter = feedAdapter
            addItemDecoration(
                DividerItemDecoration(context!!, (layoutManager as LinearLayoutManager).orientation)
            )
        }

        reportFullyDrawn()
    }
}

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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class GridRecyclerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "Grid Sample"
        setContentView(R.layout.activity_recycler_view)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val itemCount = intent.getIntExtra(EXTRA_ITEM_COUNT, 12000)
        val adapter = EntryAdapter(entries(itemCount), itemResId = R.layout.recycler_grid_cell)
        recycler.layoutManager = GridLayoutManager(this, 4)
        recycler.adapter = adapter
    }

    private fun entries(size: Int) = List(size) {
        Entry("$it")
    }

    companion object {
        const val EXTRA_ITEM_COUNT = "ITEM_COUNT"
    }
}

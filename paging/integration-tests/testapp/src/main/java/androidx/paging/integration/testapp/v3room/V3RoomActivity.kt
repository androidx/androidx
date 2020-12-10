/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.paging.integration.testapp.v3room

import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.integration.testapp.R
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class V3RoomActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_room_recycler_view)
        val viewModel by viewModels<V3RoomViewModel>()

        val adapter = V3RoomAdapter()
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        recyclerView.adapter = adapter

        lifecycleScope.launch {
            viewModel.flow.collectLatest {
                adapter.submitData(it)
            }
        }

        val addButton = findViewById<Button>(R.id.addButton)
        addButton.setOnClickListener { viewModel.insertCustomer() }

        val clearButton = findViewById<Button>(R.id.clearButton)
        clearButton.setOnClickListener { viewModel.clearAllCustomers() }
    }
}
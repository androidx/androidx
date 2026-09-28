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
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.integration.testapp.R

class V3RoomItemSnapshotActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel by viewModels<V3RoomViewModel>()

        setContent { V3RoomItemSnapshotScreen(viewModel) }
    }
}

@Composable
private fun V3RoomItemSnapshotScreen(viewModel: V3RoomViewModel) {
    val snapshotList by viewModel.asItemSnapshotFlow.collectAsStateWithLifecycle()
    val items = snapshotList.items

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        horizontal = dimensionResource(R.dimen.activity_horizontal_margin),
                        vertical = dimensionResource(R.dimen.activity_vertical_margin),
                    )
        ) {
            item { LaunchedEffect(viewModel) { viewModel.pager.prepend() } }
            items(items) { item ->
                Text(
                    text = item.name.orEmpty(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).background(Color.Blue),
                )
            }
            item { LaunchedEffect(viewModel) { viewModel.pager.append() } }
        }

        Row(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { viewModel.clearAllCustomers() }) {
                Text(stringResource(R.string.clear))
            }
            Button(onClick = { viewModel.insertCustomer() }) {
                Text(stringResource(R.string.insert))
            }
        }
    }
}

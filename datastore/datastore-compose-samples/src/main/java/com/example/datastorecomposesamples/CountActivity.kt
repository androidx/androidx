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

package com.example.datastorecomposesamples

import android.os.Bundle
import android.os.StrictMode

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.example.datastorecomposesamples.data.CountRepository
import com.example.datastorecomposesamples.data.CountState

/**
 * Main activity for displaying the counts, and allowing them to be changed.
 */
class CountActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = CountRepository.getInstance(applicationContext)

        // Strict mode allows us to check that no writes or reads are blocking the UI thread.
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .penaltyDeath()
                .build()
        )

        setContent {
            val coroutineScope = rememberCoroutineScope()
            val countState: CountState by repo.countStateFlow.collectAsState(
                CountState(0),
                coroutineScope.coroutineContext
            )
            val countProtoState: CountState by repo.countProtoStateFlow.collectAsState(
                CountState(0),
                coroutineScope.coroutineContext
            )
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    color = MaterialTheme.colors.background
                ) {
                    Column {
                        Counters(
                            title = getString(R.string.preference_counter),
                            count = countState.count,
                            onIncrement = repo::incrementPreferenceCount,
                            onDecrement = repo::decrementPreferenceCount
                        )
                        Divider()
                        Counters(
                            title = getString(R.string.proto_counter),
                            count = countProtoState.count,
                            onIncrement = repo::incrementProtoCount,
                            onDecrement = repo::decrementProtoCount
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Counters(title: String, count: Int, onIncrement: () -> Unit, onDecrement: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        Text(title, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Button(onClick = onDecrement) {
                Text(stringResource(id = R.string.count_minus))
            }
            Text(text = "${stringResource(R.string.count_colon)} $count")
            Button(onClick = onIncrement) {
                Text(stringResource(id = R.string.count_plus))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        Counters("test", 1, {}, {})
    }
}
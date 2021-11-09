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

package androidx.paging.compose.demos.room

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemsIndexed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun PagingRoomDemo() {
    val context = LocalContext.current
    val dao: UserDao = AppDatabase.getInstance(context).userDao()
    val scope = rememberCoroutineScope()

    val pageSize = 15
    val pager = remember {
        Pager(
            PagingConfig(
                pageSize = pageSize,
                enablePlaceholders = true,
                maxSize = 200
            )
        ) {
            dao.allUsers()
        }
    }

    Column {
        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val name = Names[Random.nextInt(Names.size)]
                    dao.insert(User(id = 0, name = name))
                }
            }
        ) {
            Text("Add random user")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    dao.clearAll()
                }
            }
        ) {
            Text("Clear all users")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val randomUser = dao.getRandomUser()
                    if (randomUser != null) {
                        dao.delete(randomUser)
                    }
                }
            }
        ) {
            Text("Remove random user")
        }

        Button(
            onClick = {
                scope.launch(Dispatchers.IO) {
                    val randomUser = dao.getRandomUser()
                    if (randomUser != null) {
                        val newName = Names[Random.nextInt(Names.size)]
                        val updatedUser = User(
                            randomUser.id,
                            newName
                        )
                        dao.update(updatedUser)
                    }
                }
            }
        ) {
            Text("Update random user")
        }

        val lazyPagingItems = pager.flow.collectAsLazyPagingItems()
        LazyColumn {
            itemsIndexed(
                items = lazyPagingItems,
                key = { _, user -> user.id }
            ) { index, user ->
                var counter by rememberSaveable { mutableStateOf(0) }
                Text(
                    text = "counter=$counter index=$index ${user?.name} ${user?.id}",
                    fontSize = 50.sp,
                    modifier = Modifier.clickable { counter++ }
                )
            }
        }
    }
}

val Names = listOf(
    "John",
    "Jack",
    "Ben",
    "Sally",
    "Tom",
    "Jinny",
    "Mark",
    "Betty",
    "Liam",
    "Noah",
    "Olivia",
    "Emma",
    "Ava"
)
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.LifecycleEventObserver
import platform.UIKit.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/*
 * Copyright 2023 The Android Open Source Project
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

val NativeModalWithNaviationExample = Screen.Example("Native modal with navigation") {
    NativeModalWithNavigation()
}
@Composable
private fun NativeModalWithNavigation() {
    Box(Modifier.fillMaxSize().background(Color.DarkGray), contentAlignment = Alignment.Center) {
        val viewController = LocalUIViewController.current
        Button(onClick = {
            val navigationController = UINavigationController(rootViewController = ComposeUIViewController {
                NativeNavigationPage()
            })

            viewController.presentViewController(navigationController, true, null)
        }) {
            Text("Present popup")
        }
    }
}

@Composable
private fun NativeNavigationPage() {
    val lifecycleOwner = LocalLifecycleOwner.current

    val states = remember { mutableStateListOf<String>() }

    val observer = remember {
        LifecycleEventObserver { _, event ->
            println(event)
            states.add("${states.size} ${event.name}")
        }
    }

    DisposableEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(Modifier.fillMaxSize().background(Color.DarkGray), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        val navigationController = LocalUIViewController.current.navigationController

        LazyRow(Modifier.height(50.dp).fillMaxWidth()) {
            items(states.size) { index ->
                Box(Modifier.background(Color.White).padding(16.dp)) {
                    Text(states[index], color = Color.Black)
                }
            }
        }

        Button(onClick = {
            navigationController?.pushViewController(
                ComposeUIViewController {
                    NativeNavigationPage()
                }, true
            )
        }) {
            Text("Push")
        }

        LazyVerticalGrid(GridCells.Fixed(2)) {
            items(10) { index ->
                var image by remember { mutableStateOf<UIImage?>(null) }

                DisposableEffect(Unit) {
                    val url = NSURL(string = "https://cataas.com/cat/says/$index")
                    val task = NSURLSession.sharedSession.dataTaskWithURL(url, completionHandler = { data, response, error ->
                        if (data == null) {
                            return@dataTaskWithURL
                        }

                        image = UIImage(data = data)
                    })
                    task.resume()

                    onDispose {
                        task.cancel()
                    }
                }

                UIKitView(
                    factory = {
                        val view = UIImageView()
                        view.contentMode = UIViewContentMode.UIViewContentModeScaleAspectFill
                        view.clipsToBounds = true
                        view
                    },
                    Modifier.height(80.dp),
                    update = {
                        it.image = image
                    }
                )
            }
        }
    }
}

/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.integration.a2ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.integration.a2ui.model.UiComponent
import androidx.compose.material3.integration.a2ui.ui.ComponentDetailScreen
import androidx.compose.material3.integration.a2ui.ui.ComponentListScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

class DemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DemoTheme { A2uiDemoApp() } }
    }
}

@Composable
fun A2uiDemoApp() {
    var selectedComponent by rememberSaveable { mutableStateOf<UiComponent?>(null) }

    BackHandler(enabled = selectedComponent != null) { selectedComponent = null }

    AnimatedContent(
        targetState = selectedComponent,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally(tween(SlideTransitionDuration)) { width -> width / 4 } +
                        fadeIn(tween(SlideTransitionDuration)))
                    .togetherWith(
                        slideOutHorizontally(tween(SlideTransitionDuration)) { width ->
                            -width / 4
                        } + fadeOut(tween(SlideTransitionDuration))
                    )
            } else {
                (slideInHorizontally(tween(SlideTransitionDuration)) { width -> -width / 4 } +
                        fadeIn(tween(SlideTransitionDuration)))
                    .togetherWith(
                        slideOutHorizontally(tween(SlideTransitionDuration)) { width ->
                            width / 4
                        } + fadeOut(tween(SlideTransitionDuration))
                    )
            }
        },
        label = "ScreenTransition",
    ) { component ->
        if (component != null) {
            ComponentDetailScreen(component = component, onBack = { selectedComponent = null })
        } else {
            ComponentListScreen(
                onComponentSelected = { newComponent -> selectedComponent = newComponent }
            )
        }
    }
}

private const val SlideTransitionDuration = 350

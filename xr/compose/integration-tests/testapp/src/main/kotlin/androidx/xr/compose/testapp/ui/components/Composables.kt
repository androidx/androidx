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

package androidx.xr.compose.testapp.ui.components

import android.view.Choreographer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.compose.subspace.SpatialPanel
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.movable
import androidx.xr.compose.subspace.layout.size
import androidx.xr.compose.testapp.R
import androidx.xr.compose.testapp.ui.theme.Purple40
import androidx.xr.compose.testapp.ui.theme.Purple80
import androidx.xr.compose.testapp.ui.theme.PurpleGrey40
import androidx.xr.compose.testapp.ui.theme.PurpleGrey80
import androidx.xr.compose.unit.DpVolumeSize

@Composable
fun CUJButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        colors =
            ButtonColors(
                containerColor = Purple80,
                contentColor = Color.Black,
                disabledContainerColor = Color.LightGray,
                disabledContentColor = Color.Gray,
            ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(2.dp),
            style =
                TextStyle(
                    fontSize = 20.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

@Composable
fun TestCaseButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(2.dp).fillMaxWidth(),
        shape = RoundedCornerShape(3.dp),
        colors =
            ButtonColors(
                containerColor = PurpleGrey80,
                contentColor = Color.Black,
                disabledContentColor = Color.Gray,
                disabledContainerColor = Color.DarkGray,
            ),
    ) {
        Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CommonTestPanel(
    size: DpVolumeSize,
    title: String,
    showBottomBar: Boolean,
    onClickBackArrow: (() -> Unit)?,
    onClickRecreate: (() -> Unit)? = null,
    composable: @Composable (padding: PaddingValues) -> Unit,
) {
    SpatialPanel(modifier = SubspaceModifier.size(size).movable()) {
        CommonTestScaffold(title, showBottomBar, "", onClickBackArrow, onClickRecreate, composable)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonTestScaffold(
    title: String,
    showBottomBar: Boolean,
    bottomBarText: String = "",
    onClickBackArrow: (() -> Unit)? = null,
    onClickRecreate: (() -> Unit)? = null,
    content: @Composable (padding: PaddingValues) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = { TopBarWithBackArrow(scrollBehavior, title, onClickBackArrow) },
            bottomBar = {
                if (showBottomBar)
                    Box(contentAlignment = Alignment.CenterStart) {
                        NoActionBottomBar(bottomBarText)
                        FpsCounterScreen()
                    }
            },
            floatingActionButton = {
                if (onClickRecreate != null) {
                    RecreateButton(onClickRecreate)
                }
            },
            floatingActionButtonPosition = FabPosition.EndOverlay,
            content = content,
        )
    }
}

@Composable
fun FpsCounterScreen() {
    val fps = remember { mutableIntStateOf(0) }
    val lastFrameTime = remember { mutableLongStateOf(0L) }

    // Use a LaunchedEffect to manage the Choreographer callback lifecycle
    LaunchedEffect(Unit) {
        val frameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (lastFrameTime.longValue != 0L) {
                        val frameDeltaNanos = frameTimeNanos - lastFrameTime.longValue
                        if (frameDeltaNanos > 0) {
                            val currentFps = (1_000_000_000L / frameDeltaNanos).toInt()
                            // Smooth the FPS a bit to avoid rapid fluctuations
                            fps.intValue = (fps.intValue * 0.8 + currentFps * 0.2).toInt()
                        }
                    }
                    lastFrameTime.longValue = frameTimeNanos
                    Choreographer.getInstance().postFrameCallback(this) // Post for the next frame
                }
            }

        Choreographer.getInstance().postFrameCallback(frameCallback)
    }
    // Display the FPS using a Text composable.
    // The Text composable will recompose automatically whenever 'fps' state changes.
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text(
            text = "FPS: ${fps.intValue}",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier =
                Modifier.background(Color.Black) // Semi-transparent black background
                    .padding(4.dp), // Padding around the text
        )
    }
}

@Composable
fun RecreateButton(onClickRecreate: () -> Unit) {
    FloatingActionButton(
        onClick = onClickRecreate,
        containerColor = PurpleGrey40,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
    ) {
        Image(painter = painterResource(id = R.drawable.recreate), contentDescription = "Recreate")
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopBarWithBackArrow(
    scrollBehavior: TopAppBarScrollBehavior?,
    title: String,
    onClick: (() -> Unit)?,
) {
    val fontSize = if (onClick == null) 20.sp else 32.sp
    if (scrollBehavior == null) TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    CenterAlignedTopAppBar(
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Purple40,
                titleContentColor = Color.White,
            ),
        title = { Text(title, overflow = TextOverflow.Ellipsis, fontSize = fontSize) },
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.jetpack_compose),
                    contentDescription = "Localized description",
                    tint = Color.Unspecified,
                )
            }
        },
        navigationIcon = {
            if (onClick != null) {
                IconButton(onClick = onClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        "backIcon",
                        Modifier.size(36.dp),
                        tint = Color.White,
                    )
                }
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
fun NoActionBottomBar(text: String) {
    BottomAppBar(
        actions = {
            Box {
                Text(
                    text,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = Color.Black,
                    style = TextStyle(fontSize = 20.sp),
                )
            }
        },
        containerColor = Purple40,
        tonalElevation = 5.dp,
    )
}

@Composable
fun ColumnWithCenterText(padding: PaddingValues = PaddingValues(0.dp, 0.dp, 0.dp), text: String) {
    Column(
        modifier = Modifier.background(color = Purple80).fillMaxSize().padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = Color.Black,
            style = TextStyle(fontSize = 20.sp),
        )
    }
}

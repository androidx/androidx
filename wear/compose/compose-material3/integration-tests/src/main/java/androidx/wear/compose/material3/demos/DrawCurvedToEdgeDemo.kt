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

package androidx.wear.compose.material3.demos

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AngularDirection
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.DrawCurvedToEdgeDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Slider
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.drawCurvedToEdge

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun SmallArcDrawCurvedToEdgeDemo() {
    Box(Modifier.fillMaxSize()) {
        // Default sweep is DrawCurvedToEdgeDefaults.ScrollableContentMaxSweepAngle (70 degrees)
        Text(
            "Long text that will be cut for sure.",
            Modifier.drawCurvedToEdge(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun LargeArcDrawCurvedToEdgeDemo() {
    Box(Modifier.fillMaxSize()) {
        // Static content can use DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle (120 degrees)
        Text(
            "Long text that will be cut for sure.",
            Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DrawCurvedToEdgeTextAnchorDemo() {
    var anchor by remember { mutableFloatStateOf(270f) }

    Text(
        "Modifier.drawCurvedToEdge Text Example, with a changing anchor.",
        Modifier.drawCurvedToEdge(360f, anchor = anchor),
    )
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Slider(anchor, { anchor = it }, 72, Modifier.fillMaxWidth(0.8f), valueRange = 0f..360f)
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DrawCurvedToEdgeTextSweepAngleDemo() {
    var sweepAngle by remember {
        mutableFloatStateOf(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle)
    }

    Text(
        "Modifier.drawCurvedToEdge Text Example, with a changing sweep angle.",
        Modifier.drawCurvedToEdge(sweepAngle),
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
    )
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Slider(
            sweepAngle,
            { sweepAngle = it },
            72,
            Modifier.fillMaxWidth(0.8f),
            valueRange = 0f..360f,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CounterClockwiseDrawCurvedToEdgeTextDemo() {
    Text(
        "Modifier.drawCurvedToEdge",
        Modifier.drawCurvedToEdge(
            DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle,
            AngularDirection.CounterClockwise,
            90f,
        ),
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CursiveDrawCurvedToEdgeTextDemo() {
    Text("नमस्कार जीवलोक", Modifier.drawCurvedToEdge())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun CursiveRTLDrawCurvedToEdgeTextDemo() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text("مرحبا 👋 بالعالم 🌏!", Modifier.drawCurvedToEdge())
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DrawCurvedToEdgeImageDemo() {
    var curve by remember { mutableStateOf(false) }
    val modifier = if (curve) Modifier.drawCurvedToEdge() else Modifier

    Column(Modifier.fillMaxSize(), Arrangement.Top, Alignment.CenterHorizontally) {
        Box(modifier.paint(painterResource(R.drawable.backgroundsplitimage)))
        Button(onClick = { curve = !curve }) { Text("Toggle") }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DrawCurvedToEdgeRowDemo() {
    val style = MaterialTheme.typography.titleSmall
    val height = 35.dp
    var checked by remember { mutableStateOf(false) }
    var sweepAngle by remember {
        mutableFloatStateOf(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle)
    }
    Row(
        Modifier.drawCurvedToEdge(sweepAngle)
            .padding(PaddingValues(2.dp))
            .background(Color(0xff0077ff), RoundedCornerShape(50))
            // Height is 5dp more than in CurvedLayout for the same size
            .height(height)
            .padding(horizontal = 15.dp)
    ) {
        Box(Modifier.paint(painterResource(R.drawable.backgroundsplitimage)).size(height))
        Text("drawCurvedToEdge Text", style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
        SwitchButton(checked, { checked = !checked }) { Text("Toggle button") }
        Text("More Text", style = style, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Slider(
            sweepAngle,
            { sweepAngle = it },
            72,
            Modifier.fillMaxWidth(0.7f),
            valueRange = 0f..360f,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun FourCirclesDemo() {
    @Composable
    fun curvedText(text: String, counterClockwise: Boolean = false) {
        Text(
            text,
            if (counterClockwise)
                Modifier.drawCurvedToEdge(
                    DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle,
                    AngularDirection.CounterClockwise,
                    90f,
                )
            else Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.size(160.dp).align(Alignment.Center)) {
            val commonModifier = Modifier.size(80.dp).border(2.dp, Color.Red, CircleShape)

            Column(commonModifier.align(Alignment.TopStart)) { curvedText("Top Start") }
            Column(commonModifier.align(Alignment.TopEnd)) { curvedText("Top End", true) }
            Column(commonModifier.align(Alignment.BottomStart)) { curvedText("Bottom Start") }
            Column(commonModifier.align(Alignment.BottomEnd)) { curvedText("Bottom End", true) }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun DualCurvesDemo() {
    var checked by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.drawCurvedToEdge(210f)) {
            Row(
                Modifier.padding(PaddingValues(2.dp))
                    .background(Color(0xff0077ff), RoundedCornerShape(50))
                    .padding(horizontal = 15.dp)
                    .border(2.dp, Color.Green)
            ) {
                Text("Top row", style = MaterialTheme.typography.titleSmall)
                Box(Modifier.paint(painterResource(R.drawable.backgroundsplitimage)))
                // The clickable bounds for this button is not at where it's displayed
                SwitchButton(checked, { checked = !checked }) { Text("Toggle button") }
                Box(Modifier.padding(2.dp).clip(RectangleShape).background(Color.Yellow))
            }
        }
        Box(Modifier.drawCurvedToEdge(120f, AngularDirection.CounterClockwise, 90f)) {
            Row(
                Modifier.padding(PaddingValues(1.dp))
                    .background(Color(0x19ff34ff), RoundedCornerShape(80))
                    .padding(horizontal = 2.dp)
                    .border(2.dp, Color.Red)
            ) {
                // The clickable bounds for this button is not at where it's displayed
                SwitchButton(checked, { checked = !checked }) { Text("Toggle") }
                Text(
                    "This is the bottom row",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.StartEllipsis,
                )
            }
        }
    }
}

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

package androidx.wear.compose.material3.samples

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.DrawCurvedToEdgeDefaults
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.drawCurvedToEdge

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Sampled
@Composable
fun DrawCurvedToEdgeSample() {
    Text("Curved Text", Modifier.drawCurvedToEdge())
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Sampled
@Composable
fun InsetDrawCurvedToEdgeSample() {
    // Pad the parent to draw the arc further in from the edge. Padding the curved content itself
    // would only shrink it.
    Box(Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            "A really long piece of curved text",
            Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
        )
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Sampled
@Composable
fun IconsDrawCurvedToEdgeSample() {
    val icons =
        listOf(
            R.drawable.ic_favorite_rounded to "Favourite",
            R.drawable.ic_headphones to "Headphones",
            R.drawable.ic_wifi_on to "Wifi On",
            R.drawable.ic_volume_up to "Volume Up",
            R.drawable.ic_account_circle to "Avatar",
        )
    Row(Modifier.drawCurvedToEdge()) {
        for ((resId, desc) in icons) {
            Icon(
                painter = painterResource(resId),
                contentDescription = desc,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Sampled
@Composable
fun MixedContentDrawCurvedToEdgeSample() {
    // Anything that draws can be curved, not just text.
    Row(
        Modifier.drawCurvedToEdge(DrawCurvedToEdgeDefaults.StaticContentMaxSweepAngle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_favorite_rounded),
            contentDescription = "Favourite",
            modifier = Modifier.size(24.dp),
        )
        Text("Mixed content", Modifier.padding(horizontal = 4.dp))
        Box(Modifier.size(16.dp).background(Color.Red, CircleShape))
    }
}

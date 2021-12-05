/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.AlertDialog
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ConfirmationDialog
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

@Sampled
@Composable
fun AlertDialogWithButtons() {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text("Title text displayed here", textAlign = TextAlign.Center) },
        negativeButton = { Button(onClick = {
            /* Do something e.g. navController.popBackStack()*/
        }) { Text("No") } },
        positiveButton = { Button(onClick = {
            /* Do something e.g. navController.popBackStack()*/
        }) { Text("Yes") } },
    ) {
        Text(
            text = "Body text displayed here " +
                   "(swipe right to dismiss)",
            textAlign = TextAlign.Center
        )
    }
}

@Sampled
@Composable
fun AlertDialogWithChips() {
    AlertDialog(
        icon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                contentDescription = "airplane",
                modifier = Modifier.size(24.dp).wrapContentSize(align = Alignment.Center),
            )
        },
        title = { Text(text = "Example Title Text", textAlign = TextAlign.Center) },
        message = {
            Text(
                text = "Message content goes here " +
                    "(swipe right to dismiss)",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        },
    ) {
        CompactChip(
            label = { Text("Primary") },
            onClick = { /* Do something e.g. navController.popBackStack() */ },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.width(100.dp)
        )
        Spacer(Modifier.fillMaxWidth().height(4.dp))
        CompactChip(
            label = { Text("Secondary") },
            onClick = { /* Do something e.g. navController.popBackStack() */ },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.width(100.dp)
        )
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Sampled
@Composable
fun ConfirmationDialogWithAnimation() {
    val animation = AnimatedImageVector.animatedVectorResource(R.drawable.open_on_phone_animation)
    ConfirmationDialog(
        onTimeout = {
            /* Do something e.g. navController.popBackStack() */
        },
        icon = {
            // Initially, animation is static and shown at the start position (atEnd = false).
            // Then, we use the EffectAPI to trigger a state change to atEnd = true,
            // which plays the animation from start to end.
            var atEnd by remember { mutableStateOf(false) }
            DisposableEffect(Unit) {
                atEnd = true
                onDispose {}
            }
            Image(
                painter = rememberAnimatedVectorPainter(animation, atEnd),
                contentDescription = "Open on phone",
                modifier = Modifier.size(64.dp)
            )
        },
        durationMillis = animation.totalDuration * 2L,
    ) {
        Text(
            text = "Body text displayed here " +
                "(swipe right to dismiss)",
            textAlign = TextAlign.Center
        )
    }
}

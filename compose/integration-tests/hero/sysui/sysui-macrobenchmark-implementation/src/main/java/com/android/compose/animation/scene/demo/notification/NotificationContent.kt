/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo.notification

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.ValueKey
import com.android.compose.animation.scene.animateElementIntAsState
import com.android.compose.animation.scene.demo.CachedText
import com.android.compose.animation.scene.transitions

object NotificationContent {
    object Elements {
        val Icon = ElementKey("NotificationContentIcon")
        val Title = ElementKey("NotificationContentTitle")
        val AltTitle = ElementKey("NotificationContentAltTitle")
        val Content = ElementKey("NotificationContentContent")
        val Chevron = ElementKey("NotificationContentChevron")
    }

    object Values {
        val ChevronRotation = ValueKey("NotificationChevronRotation")
    }

    fun transitions() = transitions {
        from(Notification.Scenes.Expanded, to = Notification.Scenes.Collapsed) {
            spec = tween(500)

            fractionRange(end = 0.5f) { fade(Elements.AltTitle) }
        }
    }
}

@Composable
fun ContentScope.CollapsedNotificationContent(
    i: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.height(80.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Home, Modifier.zIndex(-2f))
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Title(i, textMeasurer)
            Spacer(Modifier.width(8.dp))
            Content("This is the notification content", textMeasurer)
        }
        Spacer(Modifier.width(8.dp))
        Chevron(rotate = false, Modifier.zIndex(-1f))
    }
}

@Composable
fun ContentScope.ExpandedNotificationContent(
    i: Int,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Home)
            Spacer(Modifier.width(8.dp))
            Text(
                "Example notification",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).element(NotificationContent.Elements.AltTitle),
            )
            Spacer(Modifier.width(8.dp))
            Chevron(rotate = true)
        }
        Row {
            Spacer(Modifier.size(24.dp + 8.dp))
            Column {
                Title(i, textMeasurer)
                Spacer(Modifier.width(8.dp))
                Content("This is the notification content", textMeasurer)
            }
        }
    }
}

@Composable
private fun ContentScope.Title(i: Int, textMeasurer: TextMeasurer, modifier: Modifier = Modifier) {
    CachedText(
        "Notification $i",
        textMeasurer,
        modifier.element(NotificationContent.Elements.Title),
        style = MaterialTheme.typography.titleSmall,
    )
}

@Composable
private fun ContentScope.Content(
    content: String,
    textMeasurer: TextMeasurer,
    modifier: Modifier = Modifier,
) {
    CachedText(
        content,
        textMeasurer,
        modifier.element(NotificationContent.Elements.Content),
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ContentScope.Icon(icon: ImageVector, modifier: Modifier = Modifier) {
    Icon(icon, null, modifier.size(24.dp).element(NotificationContent.Elements.Icon))
}

@Composable
private fun ContentScope.Chevron(rotate: Boolean, modifier: Modifier = Modifier) {
    val key = NotificationContent.Elements.Chevron
    ElementWithValues(key, modifier) {
        val rotation by
            animateElementIntAsState(
                if (rotate) 180 else 0,
                NotificationContent.Values.ChevronRotation,
            )

        content {
            Icon(
                Icons.Default.ExpandMore,
                null,
                Modifier.size(24.dp).drawWithContent {
                    rotate(rotation.toFloat()) { this@drawWithContent.drawContent() }
                },
            )
        }
    }
}

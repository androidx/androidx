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

package androidx.wear.compose.integration.demos

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.ScalingLazyColumnWithRSB
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.OutlinedButton
import androidx.wear.compose.material.OutlinedCompactButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton

@Composable
fun ButtonSizes() {
    var enabled by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {},
                enabled = enabled,
                modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
            ) {
                Text("Lrg")
            }
            Button(
                onClick = {},
                enabled = enabled,
            ) {
                // NB Leave size as default for this one.
                Text("Def")
            }
            Button(
                onClick = {},
                enabled = enabled,
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
            ) {
                Text("Sml")
            }
            CompactButton(
                onClick = {},
                enabled = enabled,
            ) {
                Text("XS")
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Enabled",
                style = MaterialTheme.typography.caption2,
                color = Color.White
            )
            ToggleButton(
                checked = enabled,
                onCheckedChange = {
                    enabled = it
                },
                modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
            ) {
                Text(text = if (enabled) "Yes" else "No")
            }
        }
    }
}

@Composable
fun ButtonStyles() {
    var enabled by remember { mutableStateOf(true) }
    val context = LocalContext.current

    ScalingLazyColumnWithRSB(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            ListHeader {
                Text(
                    text = "Styles (Click for details)",
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Secondary, $enabled", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    enabled = enabled,
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Primary colors", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.primaryButtonColors(),
                    enabled = enabled,
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Small, icon only, $enabled", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.iconButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                    enabled = enabled
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Large, icon only, $enabled", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.iconButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                    enabled = enabled
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Outlined, $enabled", Toast.LENGTH_LONG
                        ).show()
                    },
                    enabled = enabled,
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
                OutlinedCompactButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Outlined Compact, $enabled", Toast.LENGTH_LONG
                        ).show()
                    },
                    enabled = enabled,
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Custom Shape", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier,
                    enabled = enabled,
                    shape = CutCornerShape(4.dp)
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
                CompactButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Compact Button with Custom Shape", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier,
                    enabled = enabled,
                    shape = CutCornerShape(4.dp)
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedCompactButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Outlined Compact Button with Custom Shape", Toast.LENGTH_LONG
                        ).show()
                    },
                    modifier = Modifier,
                    enabled = enabled,
                    shape = CutCornerShape(4.dp)
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
                Button(
                    onClick = {
                        Toast.makeText(
                            context,
                            "Button: Override primary colors", Toast.LENGTH_LONG
                        ).show()
                    },
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = AlternatePrimaryColor1,
                    ),
                    enabled = enabled,
                ) {
                    DemoIcon(R.drawable.ic_accessibility_24px)
                }
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Buttons Enabled",
                    style = MaterialTheme.typography.caption2,
                    color = Color.White
                )
                ToggleButton(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                    },
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    DemoIcon(R.drawable.ic_check_24px)
                }
            }
        }
    }
}

@Composable
fun ButtonGallery() {
    val state = rememberScalingLazyListState()
    ScalingLazyColumnWithRSB(
        state = state,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ListHeader { Text("Large - 60 x 60") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_play, contentDescription = "Play")
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_play, contentDescription = "Play")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.iconButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_skip_next, contentDescription = "Skip")
                }
                Spacer(Modifier.width(ButtonDefaults.LargeButtonSize))
            }
        }

        item {
            ListHeader { Text("Default - 52 x 52") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors(),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_check_24px, contentDescription = "Tick")
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_clear_24px,
                        contentDescription = "Cross"
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.iconButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_devices_24,
                        contentDescription = "Devices"
                    )
                }
                Spacer(Modifier.width(ButtonDefaults.DefaultButtonSize))
            }
        }

        item {
            ListHeader { Text("Small - 48 x 48") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_music_24, contentDescription = "Music")
                }
                Button(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    DemoIcon(resourceId = R.drawable.ic_mic_24, contentDescription = "Mic")
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.iconButtonColors(),
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_volume_up_24px,
                        contentDescription = "Vol"
                    )
                }
                Spacer(Modifier.width(ButtonDefaults.SmallButtonSize))
            }
        }

        item {
            ListHeader { Text("Extra small - 32 x 32") }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.primaryButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_arrow_left_modified,
                        contentDescription = "Left",
                    )
                }
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.secondaryButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_arrow_right_modified,
                        contentDescription = "Right",
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.iconButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_arrow_left_modified,
                        contentDescription = "Left",
                    )
                }
                CompactButton(
                    onClick = {},
                    colors = ButtonDefaults.iconButtonColors(),
                ) {
                    DemoIcon(
                        resourceId = R.drawable.ic_arrow_right_modified,
                        contentDescription = "Right",
                    )
                }
            }
        }
    }
}

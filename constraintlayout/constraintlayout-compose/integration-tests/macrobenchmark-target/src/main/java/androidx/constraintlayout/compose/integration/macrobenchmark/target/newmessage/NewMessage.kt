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

@file:OptIn(ExperimentalMotionApi::class)

package androidx.constraintlayout.compose.integration.macrobenchmark.target.newmessage

import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.ExperimentalMotionApi
import androidx.constraintlayout.compose.InvalidationStrategy
import androidx.constraintlayout.compose.MotionLayout
import androidx.constraintlayout.compose.MotionLayoutScope
import androidx.constraintlayout.compose.MotionScene
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.integration.macrobenchmark.target.common.components.TestableButton

// Copied from ComposeMail project

@Preview
@Composable
fun NewMotionMessagePreview() {
    NewMotionMessageWithControls(useDsl = false, optimize = false)
}

@Preview
@Composable
fun NewMotionMessagePreviewWithDsl() {
    NewMotionMessageWithControls(useDsl = true, optimize = false)
}

@Preview
@Composable
fun NewMotionMessagePreviewWithDslOptimized() {
    NewMotionMessageWithControls(useDsl = true, optimize = true)
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMotionApi::class)
@Composable
fun NewMotionMessageWithControls(
    useDsl: Boolean,
    optimize: Boolean
) {
    val initialLayout = NewMessageLayout.Full
    val newMessageState = rememberNewMessageState(initialLayoutState = initialLayout)
    val motionScene = if (useDsl) {
        messageMotionSceneDsl(initialState = initialLayout)
    } else {
        messageMotionScene(initialState = initialLayout)
    }
    Column(Modifier.semantics { testTagsAsResourceId = true }) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TestableButton(
                onClick = newMessageState::setToFab,
                text = "Fab"
            )
            TestableButton(
                onClick = newMessageState::setToFull,
                text = "Full"
            )
            TestableButton(
                onClick = newMessageState::setToMini,
                text = "Mini"
            )
        }
        val invalidationStrategy = remember(newMessageState, optimize) {
            if (optimize) {
                InvalidationStrategy {
                    newMessageState.currentState
                }
            } else {
                InvalidationStrategy.DefaultInvalidationStrategy
            }
        }
        NewMessageButton(
            modifier = Modifier.fillMaxSize(),
            motionScene = motionScene,
            state = newMessageState,
            invalidationStrategy = invalidationStrategy,
        )
    }
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun messageMotionSceneDsl(initialState: NewMessageLayout): MotionScene {
    val startState = remember { initialState }
    val endState = when (startState) {
        NewMessageLayout.Fab -> NewMessageLayout.Full
        NewMessageLayout.Mini -> NewMessageLayout.Fab
        NewMessageLayout.Full -> NewMessageLayout.Fab
    }

    val primary = MaterialTheme.colors.primary
    val primaryVariant = MaterialTheme.colors.primaryVariant
    val onPrimary = MaterialTheme.colors.onPrimary
    val surface = MaterialTheme.colors.surface
    val onSurface = MaterialTheme.colors.onSurface

    return MotionScene {
        val box = createRefFor("box")
        val minIcon = createRefFor("minIcon")
        val editClose = createRefFor("editClose")
        val title = createRefFor("title")
        val content = createRefFor("content")

        val fab = constraintSet(NewMessageLayout.Fab.name) {
            constrain(box) {
                width = Dimension.value(50.dp)
                height = Dimension.value(50.dp)
                end.linkTo(parent.end, 12.dp)
                bottom.linkTo(parent.bottom, 12.dp)
                customColor("background", primary)
            }
            constrain(minIcon) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                end.linkTo(editClose.start, 8.dp)
                top.linkTo(editClose.top)
                customColor("content", onPrimary)
            }
            constrain(editClose) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                centerTo(box)

                customColor("content", onPrimary)
            }
            constrain(title) {
                width = Dimension.fillToConstraints
                top.linkTo(box.top)
                bottom.linkTo(editClose.bottom)
                start.linkTo(box.start, 8.dp)
                end.linkTo(minIcon.start, 8.dp)
                customColor("content", onPrimary)

                visibility = Visibility.Gone
            }
            constrain(content) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                start.linkTo(box.start, 8.dp)
                end.linkTo(box.end, 8.dp)

                top.linkTo(editClose.bottom, 8.dp)
                bottom.linkTo(box.bottom, 8.dp)

                visibility = Visibility.Gone
            }
        }
        val full = constraintSet(NewMessageLayout.Full.name) {
            constrain(box) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                start.linkTo(parent.start, 12.dp)
                end.linkTo(parent.end, 12.dp)
                bottom.linkTo(parent.bottom, 12.dp)
                top.linkTo(parent.top, 40.dp)
                customColor("background", surface)
            }
            constrain(minIcon) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                end.linkTo(editClose.start, 8.dp)
                top.linkTo(editClose.top)
                customColor("content", onSurface)
            }
            constrain(editClose) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                end.linkTo(box.end, 4.dp)
                top.linkTo(box.top, 4.dp)
                customColor("content", onSurface)
            }
            constrain(title) {
                width = Dimension.fillToConstraints
                top.linkTo(box.top)
                bottom.linkTo(editClose.bottom)
                start.linkTo(box.start, 8.dp)
                end.linkTo(minIcon.start, 8.dp)
                customColor("content", onSurface)
            }
            constrain(content) {
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
                start.linkTo(box.start, 8.dp)
                end.linkTo(box.end, 8.dp)
                top.linkTo(editClose.bottom, 8.dp)
                bottom.linkTo(box.bottom, 8.dp)
            }
        }
        val mini = constraintSet(NewMessageLayout.Mini.name) {
            constrain(box) {
                width = Dimension.value(220.dp)
                height = Dimension.value(50.dp)

                end.linkTo(parent.end, 12.dp)
                bottom.linkTo(parent.bottom, 12.dp)

                customColor("background", primaryVariant)
            }
            constrain(minIcon) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                end.linkTo(editClose.start, 8.dp)
                top.linkTo(editClose.top)

                rotationZ = 180f

                customColor("content", onPrimary)
            }
            constrain(editClose) {
                width = Dimension.value(40.dp)
                height = Dimension.value(40.dp)

                end.linkTo(box.end, 4.dp)
                top.linkTo(box.top, 4.dp)
                customColor("content", onPrimary)
            }
            constrain(title) {
                width = Dimension.fillToConstraints
                top.linkTo(box.top)
                bottom.linkTo(editClose.bottom)
                start.linkTo(box.start, 8.dp)
                end.linkTo(minIcon.start, 8.dp)
                customColor("content", onPrimary)
            }
            constrain(content) {
                width = Dimension.fillToConstraints
                start.linkTo(box.start, 8.dp)
                end.linkTo(box.end, 8.dp)

                top.linkTo(editClose.bottom, 8.dp)
                bottom.linkTo(box.bottom, 8.dp)

                visibility = Visibility.Gone
            }
        }

        fun constraintSetFor(layoutState: NewMessageLayout) =
            when (layoutState) {
                NewMessageLayout.Full -> full
                NewMessageLayout.Mini -> mini
                NewMessageLayout.Fab -> fab
            }
        defaultTransition(
            from = constraintSetFor(startState),
            to = constraintSetFor(endState)
        )
    }
}

@OptIn(ExperimentalMotionApi::class)
@Composable
private fun messageMotionScene(initialState: NewMessageLayout): MotionScene {
    val startState = remember { initialState }
    val endState = when (startState) {
        NewMessageLayout.Fab -> NewMessageLayout.Full
        NewMessageLayout.Mini -> NewMessageLayout.Fab
        NewMessageLayout.Full -> NewMessageLayout.Fab
    }

    val startStateName = startState.name
    val endStateName = endState.name
    val primary = MaterialTheme.colors.primary.toHexString()
    val primaryVariant = MaterialTheme.colors.primaryVariant.toHexString()
    val onPrimary = MaterialTheme.colors.onPrimary.toHexString()
    val surface = MaterialTheme.colors.surface.toHexString()
    val onSurface = MaterialTheme.colors.onSurface.toHexString()

    return MotionScene(
        content =
        """
        {
          ConstraintSets: {
            ${NewMessageLayout.Fab.name}: {
              box: {
                width: 50, height: 50,
                end: ['parent', 'end', 12],
                bottom: ['parent', 'bottom', 12],
                custom: {
                  background: '#$primary'
                }
              },
              minIcon: {
                width: 40, height: 40,
                end: ['editClose', 'start', 8],
                top: ['editClose', 'top', 0],
                visibility: 'gone',
                custom: {
                  content: '#$onPrimary'
                }
              },
              editClose: {
                width: 40, height: 40,
                centerHorizontally: 'box',
                centerVertically: 'box',
                custom: {
                  content: '#$onPrimary'
                }
              },
              title: {
                width: 'spread',
                top: ['box', 'top', 0],
                bottom: ['editClose', 'bottom', 0],
                start: ['box', 'start', 8],
                end: ['minIcon', 'start', 8],
                custom: {
                  content: '#$onPrimary'
                }
                
                visibility: 'gone'
              },
              content: {
                width: 'spread', height: 'spread',
                start: ['box', 'start', 8],
                end: ['box', 'end', 8],
                
                top: ['editClose', 'bottom', 8],
                bottom: ['box', 'bottom', 8],
                
                visibility: 'gone'
              }
            },
            ${NewMessageLayout.Full.name}: {
              box: {
                width: 'spread', height: 'spread',
                start: ['parent', 'start', 12],
                end: ['parent', 'end', 12],
                bottom: ['parent', 'bottom', 12],
                top: ['parent', 'top', 40],
                custom: {
                  background: '#$surface'
                }
              },
              minIcon: {
                width: 40, height: 40,
                end: ['editClose', 'start', 8],
                top: ['editClose', 'top', 0],
                custom: {
                  content: '#$onSurface'
                }
              },
              editClose: {
                width: 40, height: 40,
                end: ['box', 'end', 4],
                top: ['box', 'top', 4],
                custom: {
                  content: '#$onSurface'
                }
              },
              title: {
                width: 'spread',
                top: ['box', 'top', 0],
                bottom: ['editClose', 'bottom', 0],
                start: ['box', 'start', 8],
                end: ['minIcon', 'start', 8],
                custom: {
                  content: '#$onSurface'
                }
              },
              content: {
                width: 'spread', height: 'spread',
                start: ['box', 'start', 8],
                end: ['box', 'end', 8],
                
                top: ['editClose', 'bottom', 8],
                bottom: ['box', 'bottom', 8]
              }
            },
            ${NewMessageLayout.Mini.name}: {
              box: {
                width: 180, height: 50,
                bottom: ['parent', 'bottom', 12],
                end: ['parent', 'end', 12],
                custom: {
                  background: '#$primaryVariant'
                }
              },
              minIcon: {
                width: 40, height: 40,
                end: ['editClose', 'start', 8],
                top: ['editClose', 'top', 0],
                rotationZ: 180,
                custom: {
                  content: '#$onPrimary'
                }
              },
              editClose: {
                width: 40, height: 40,
                end: ['box', 'end', 4],
                top: ['box', 'top', 4],
                custom: {
                  content: '#$onPrimary'
                }
              },
              title: {
                width: 'spread',
                top: ['box', 'top', 0],
                bottom: ['editClose', 'bottom', 0],
                start: ['box', 'start', 8],
                end: ['minIcon', 'start', 8],
                custom: {
                  content: '#$onPrimary'
                }
              },
              content: {
                width: 'spread', height: 'spread',
                start: ['box', 'start', 8],
                end: ['box', 'end', 8],
                
                top: ['editClose', 'bottom', 8],
                bottom: ['box', 'bottom', 8],
                
                visibility: 'gone'
              }
            }
          },
          Transitions: {
            default: {
              from: '$startStateName',
              to: '$endStateName'
            }
          }
        }
    """
    )
}

@OptIn(ExperimentalMotionApi::class)
@Composable
internal fun MotionLayoutScope.MotionMessageContent(
    state: NewMessageState
) {
    val currentState = state.currentState
    val focusManager = LocalFocusManager.current
    val dialogName = remember(currentState) {
        when (currentState) {
            NewMessageLayout.Mini -> "Draft"
            else -> "Message"
        }
    }
    Surface(
        modifier = Modifier.layoutId("box"),
        color = customColor(id = "box", name = "background"),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {}
    ColorableIconButton(
        modifier = Modifier.layoutId("editClose"),
        imageVector = when (currentState) {
            NewMessageLayout.Fab -> Icons.Default.Edit
            else -> Icons.Default.Close
        },
        color = customColor("editClose", "content"),
        enabled = true
    ) {
        when (currentState) {
            NewMessageLayout.Fab -> state.setToFull()
            else -> state.setToFab()
        }
    }
    ColorableIconButton(
        modifier = Modifier.layoutId("minIcon"),
        imageVector = Icons.Default.KeyboardArrowDown,
        color = customColor("minIcon", "content"),
        enabled = true
    ) {
        when (currentState) {
            NewMessageLayout.Full -> state.setToMini()
            else -> state.setToFull()
        }
    }
    CheapText(
        text = dialogName,
        modifier = Modifier.layoutId("title"),
        color = customColor("title", "content"),
        style = MaterialTheme.typography.h6
    )
    MessageWidget(modifier = Modifier.layoutId("content"), onDelete = {
        focusManager.clearFocus()
        state.setToFab()
    })
//            MessageWidgetCol(
//                modifier = Modifier
//                    .layoutId("content")
//                    .padding(start = 4.dp, end = 4.dp, bottom = 4.dp)
//            )
}

@Composable
private fun NewMessageButton(
    motionScene: MotionScene,
    state: NewMessageState,
    invalidationStrategy: InvalidationStrategy,
    modifier: Modifier = Modifier,
) {
    val currentStateName = state.currentState.name
    MotionLayout(
        motionScene = motionScene,
        animationSpec = tween(700),
        constraintSetName = currentStateName,
        modifier = modifier,
        invalidationStrategy = invalidationStrategy
    ) {
        MotionMessageContent(state = state)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ColorableIconButton(
    modifier: Modifier,
    imageVector: ImageVector,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color.Transparent,
        contentColor = color,
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// With column
@Composable
internal fun MessageWidgetCol(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Recipients")
            }
        )
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Subject")
            }
        )
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 2.0f, fill = true),
            value = "",
            onValueChange = {},
            placeholder = {
                Text("Message")
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Button(onClick = { /*TODO*/ }) {
                Row {
                    Text(text = "Send")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Default.Send,
                        contentDescription = "Send Mail",
                    )
                }
            }
            Button(onClick = { /*TODO*/ }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Draft",
                )
            }
        }
    }
}

// With ConstraintLayout
@Preview
@Composable
private fun MessageWidgetPreview() {
    MessageWidget(modifier = Modifier.fillMaxSize())
}

@Composable
internal fun MessageWidget(
    modifier: Modifier,
    onDelete: () -> Unit = {}
) {
    val constraintSet = remember {
        ConstraintSet(
            """
                {
                    gl1: { type: 'hGuideline', end: 50 },
                    recipient: {
                      top: ['parent', 'top', 2],
                      width: 'spread',
                      centerHorizontally: 'parent',
                    },
                    subject: { 
                      top: ['recipient', 'bottom', 8],
                      width: 'spread',
                      centerHorizontally: 'parent',
                    },
                    message: {
                      height: 'spread',
                      width: 'spread',
                      centerHorizontally: 'parent',
                      top: ['subject', 'bottom', 8],
                      bottom: ['gl1', 'bottom', 4],
                    },
                    delete: {
                      height: 'spread',
                      top: ['gl1', 'bottom', 0],
                      bottom: ['parent', 'bottom', 4],
                      start: ['parent', 'start', 0]
                    },
                    send: {
                      height: 'spread',
                      top: ['gl1', 'bottom', 0],
                      bottom: ['parent', 'bottom', 4],
                      end: ['parent', 'end', 0]
                    }
                }
            """.trimIndent()
        )
    }
    ConstraintLayout(
        modifier = modifier.padding(top = 4.dp, start = 8.dp, end = 8.dp, bottom = 0.dp),
        constraintSet = constraintSet
    ) {
        OutlinedTextField(
            modifier = Modifier.layoutId("recipient"),
            value = "",
            onValueChange = {},
            label = {
                CheapText("To")
            }
        )
        OutlinedTextField(
            modifier = Modifier.layoutId("subject"),
            value = "",
            onValueChange = {},
            label = {
                CheapText("Subject")
            }
        )
        OutlinedTextField(
            modifier = Modifier
                .layoutId("message")
                .fillMaxHeight(),
            value = "",
            onValueChange = {},
            label = {
                CheapText("Message")
            }
        )
        Button(
            modifier = Modifier.layoutId("send"),
            onClick = onDelete // TODO: Do something different for Send onClick
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CheapText(text = "Send")
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "Send Mail",
                )
            }
        }
        Button(
            modifier = Modifier.layoutId("delete"),
            onClick = onDelete
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Draft",
            )
        }
    }
}

/**
 * [Text] Composable constrained to one line for better animation performance.
 */
@Composable
private fun CheapText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    overflow: TextOverflow = TextOverflow.Clip
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        maxLines = 1,
        overflow = overflow,
    )
}

private fun Color.toHexString() = toArgb().toUInt().toString(16)

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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.material3.a2ui.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Button"` component. */
internal object MaterialA2uiBasicCatalogV1Button : A2uiBasicCatalogV1.Button {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        childId: String,
        variant: A2uiBasicCatalogV1.Button.Variant,
        action: Map<String, Any?>,
        modifier: Modifier,
    ) {
        val currentAction by rememberUpdatedState(action)
        val onClick: () -> Unit = remember { { dispatchAction(currentAction) } }

        val childState = observeA2uiComponentState(childId)
        val isError = childState is A2uiComponentState.Error
        val isLoading = childState is A2uiComponentState.Loading

        ButtonVariant(
            variant = variant,
            enabled = !isLoading && !isError,
            error = isError,
            onClick = onClick,
            modifier = modifier,
        ) {
            AnimatedContent(
                targetState = childState,
                transitionSpec = MaterialA2uiDefaults.transitionSpec(),
                contentKey = { state ->
                    when (state) {
                        A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success -> Pair(childId, state.component.type)
                    }
                },
                label = "ButtonChildTransition",
            ) { state ->
                when (state) {
                    is A2uiComponentState.Error -> {
                        Text(
                            text = stringResource(R.string.error),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    is A2uiComponentState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                    is A2uiComponentState.Success -> {
                        A2uiComponent(component = state.component)
                    }
                }
            }
        }
    }

    @Composable
    private fun ButtonVariant(
        variant: A2uiBasicCatalogV1.Button.Variant,
        enabled: Boolean,
        error: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        val buttonModifier = modifier.then(DefaultButtonModifier)
        val (targetDisabledContainerColor, targetDisabledContentColor) =
            if (error) {
                MaterialTheme.colorScheme.errorContainer to
                    MaterialTheme.colorScheme.onErrorContainer
            } else {
                Color.Unspecified to Color.Unspecified
            }
        val disabledContainerColor by
            animateColorAsState(
                targetValue = targetDisabledContainerColor,
                label = "ButtonDisabledContainerColor",
            )
        val disabledContentColor by
            animateColorAsState(
                targetValue = targetDisabledContentColor,
                label = "ButtonDisabledContentColor",
            )

        when (variant) {
            A2uiBasicCatalogV1.Button.Variant.Default -> {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = buttonModifier,
                    colors =
                        ButtonDefaults.outlinedButtonColors(
                            disabledContainerColor = disabledContainerColor,
                            disabledContentColor = disabledContentColor,
                        ),
                ) {
                    content()
                }
            }
            A2uiBasicCatalogV1.Button.Variant.Borderless -> {
                TextButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = buttonModifier,
                    colors =
                        ButtonDefaults.textButtonColors(
                            disabledContainerColor = disabledContainerColor,
                            disabledContentColor = disabledContentColor,
                        ),
                ) {
                    content()
                }
            }
            A2uiBasicCatalogV1.Button.Variant.Primary -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = buttonModifier,
                    colors =
                        ButtonDefaults.buttonColors(
                            disabledContainerColor = disabledContainerColor,
                            disabledContentColor = disabledContentColor,
                        ),
                ) {
                    content()
                }
            }
        }
    }
}

private val DefaultButtonModifier: Modifier =
    Modifier.padding(vertical = 8.dp).defaultMinSize(minHeight = 48.dp)

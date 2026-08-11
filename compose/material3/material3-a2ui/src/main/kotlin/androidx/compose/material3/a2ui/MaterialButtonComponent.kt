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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiComponentState
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.runtime.StaticA2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Button"` component schema.
 *
 * This component resolves a single target `child` component to display inside the button, and binds
 * its `onClick` handler to the generic `action` payload provided by the A2UI protocol. It uses
 * Material 3's [Button], [OutlinedButton], and [TextButton] depending on the `variant` property. It
 * includes built-in support for animating content changes after the inner `child` component
 * resolves.
 *
 * **Schema Properties:**
 * * `child` (ComponentId, required): The ID of the child component. Typically, a 'Text' component
 *   for a labeled button, an 'Icon' for an icon-only button, or a 'Row' containing 'Icon' and
 *   'Text' children for a button with both text and icon.
 * * `action` (Action, required): The action to perform when the button is clicked.
 * * `variant` (String Enum): A hint for the button style. If omitted, a default button style is
 *   used. 'primary' indicates this is the main call-to-action button. 'borderless' means the button
 *   has no visual border or background, making its child content appear like a clickable link.
 */
public object MaterialButtonComponent : A2uiComponent {

    private val childProp = A2uiProperty.componentId("child", required = true)
    private val actionProp = A2uiProperty.action("action", required = true)
    private val variantProp =
        A2uiProperty.stringEnum("variant", enumValues = listOf("default", "primary", "borderless"))

    override val name: String = "Button"
    override val description: String =
        "An interactive button that dispatches an action when clicked."
    override val properties: List<StaticA2uiProperty<*>> =
        listOf(childProp, actionProp, variantProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val action = properties[actionProp]
        val childId =
            properties[childProp]
                ?: throw IllegalStateException("Required property '${childProp.key}' is missing.")
        val variant = ButtonVariant.from(properties[variantProp])

        val currentAction by rememberUpdatedState(action)
        val onClick: () -> Unit = remember { { currentAction?.let { dispatchAction(it) } } }

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
                contentKey = { state ->
                    when (state) {
                        A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success -> Pair(childId, state.component.type)
                    }
                },
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
        variant: ButtonVariant,
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
            ButtonVariant.Default -> {
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
            ButtonVariant.Borderless -> {
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
            ButtonVariant.Primary -> {
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

    private enum class ButtonVariant(val stringValue: String) {
        Default("default"),
        Primary("primary"),
        Borderless("borderless");

        companion object {
            fun from(value: String?): ButtonVariant {
                return when (value) {
                    Primary.stringValue -> Primary
                    Borderless.stringValue -> Borderless
                    else -> Default
                }
            }
        }
    }
}

private val DefaultButtonModifier: Modifier =
    Modifier.padding(vertical = 8.dp).defaultMinSize(minHeight = 48.dp)

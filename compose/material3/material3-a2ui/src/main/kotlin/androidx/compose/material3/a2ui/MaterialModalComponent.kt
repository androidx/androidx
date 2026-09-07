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
import androidx.a2ui.compose.runtime.ProvideActionInterceptor
import androidx.a2ui.compose.runtime.StaticA2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Modal"` component schema.
 *
 * This component acts as a modal entry point. It displays the `trigger` child component in the
 * normal UI flow. Interacting with the trigger intercepts the click and opens a centered dialog
 * displaying the `content` child component.
 *
 * **Schema Properties:**
 * * `trigger` (ComponentId, required): The ID of the component that opens the modal when interacted
 *   with (e.g., a button).
 * * `content` (ComponentId, required): The ID of the component to be displayed inside the modal.
 */
public object MaterialModalComponent : A2uiComponent {

    private val triggerProp =
        A2uiProperty.componentId(
            key = "trigger",
            required = true,
            description =
                "The ID of the component that opens the modal when interacted with" +
                    " (e.g., a button).",
        )

    private val contentProp =
        A2uiProperty.componentId(
            key = "content",
            required = true,
            description = "The ID of the component to be displayed inside the modal.",
        )

    override val name: String = "Modal"
    override val description: String = "A dialog window."
    override val properties: List<StaticA2uiProperty<*>> = listOf(triggerProp, contentProp)

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val triggerId =
            checkNotNull(properties[triggerProp]) {
                "Required property '${triggerProp.key}' is missing."
            }
        val contentId =
            checkNotNull(properties[contentProp]) {
                "Required property '${contentProp.key}' is missing."
            }

        var isDialogOpen by rememberSaveable { mutableStateOf(false) }

        ModalTrigger(
            triggerId = triggerId,
            onOpenDialog = { isDialogOpen = true },
            modifier = modifier,
        )

        if (isDialogOpen) {
            ModalDialog(contentId = contentId, onDismissRequest = { isDialogOpen = false })
        }
    }

    @Composable
    private fun A2uiComponentScope.ModalTrigger(
        triggerId: String,
        onOpenDialog: () -> Unit,
        modifier: Modifier,
    ) {
        ProvideActionInterceptor(
            onIntercept = {
                onOpenDialog()
                // Return true to consume the action locally and open the modal without
                // dispatching the action to the server.
                true
            }
        ) {
            val triggerState = observeA2uiComponentState(triggerId)

            AnimatedContent(
                targetState = triggerState,
                transitionSpec = MaterialA2uiDefaults.transitionSpec(),
                contentKey = { state ->
                    when (state) {
                        A2uiComponentState.Loading -> "loading"
                        is A2uiComponentState.Error -> "error"
                        is A2uiComponentState.Success -> Pair(triggerId, state.component.type)
                    }
                },
                label = "ModalTriggerTransition",
                modifier = modifier,
            ) { state ->
                when (state) {
                    A2uiComponentState.Loading -> {
                        MaterialA2uiDefaults.LoadingIndicator(modifier = ModalLoadingModifier)
                    }
                    is A2uiComponentState.Error -> {
                        MaterialA2uiDefaults.ErrorFallback(
                            modifier = ModalErrorModifier,
                            exception = state.exception,
                        )
                    }
                    is A2uiComponentState.Success -> {
                        A2uiComponent(
                            component = state.component,
                            modifier =
                                Modifier.clickable(role = Role.Button, onClick = onOpenDialog),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun A2uiComponentScope.ModalDialog(contentId: String, onDismissRequest: () -> Unit) {
        BasicAlertDialog(onDismissRequest = onDismissRequest) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier =
                    Modifier.padding(
                        start = 16.dp,
                        top = 24.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    ),
            ) {
                ProvideActionInterceptor(
                    onIntercept = {
                        onDismissRequest()
                        // Return false to dismiss the modal locally while propagating the action
                        // to the server.
                        false
                    }
                ) {
                    val contentState = observeA2uiComponentState(contentId)

                    AnimatedContent(
                        targetState = contentState,
                        transitionSpec = MaterialA2uiDefaults.transitionSpec(),
                        contentKey = { state ->
                            when (state) {
                                A2uiComponentState.Loading -> "loading"
                                is A2uiComponentState.Error -> "error"
                                is A2uiComponentState.Success ->
                                    Pair(contentId, state.component.type)
                            }
                        },
                        label = "ModalContentTransition",
                    ) { state ->
                        when (state) {
                            A2uiComponentState.Loading -> {
                                MaterialA2uiDefaults.LoadingIndicator(
                                    modifier = ModalLoadingModifier
                                )
                            }
                            is A2uiComponentState.Error -> {
                                MaterialA2uiDefaults.ErrorFallback(
                                    modifier = ModalErrorModifier,
                                    exception = state.exception,
                                )
                            }
                            is A2uiComponentState.Success -> {
                                A2uiComponent(component = state.component)
                            }
                        }
                    }
                }
            }
        }
    }
}

private val ModalLoadingModifier = Modifier.fillMaxWidth().height(48.dp)
private val ModalErrorModifier = Modifier.fillMaxWidth()

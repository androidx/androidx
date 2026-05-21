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

package androidx.compose.remote.testing

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] that allows you to set a Remote Compose content without the necessity to provide a
 * host for the content. The host, such as an Activity, will be created by the test rule.
 *
 * The [CoreDocument] player implementation should be provided, giving the flexibility for
 * developers to choose their own implementation.
 *
 * @param _composeTestRule [ComposeContentTestRule] to be used by this [TestRule].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBaseContentTestRule(
    private val _composeTestRule: ComposeContentTestRule? = null
) : TestRule {

    /** [ComposeContentTestRule] used by this [TestRule]. */
    public val composeTestRule: ComposeContentTestRule =
        _composeTestRule ?: createComposeRule(StandardTestDispatcher())

    override fun apply(base: Statement, description: Description): Statement =
        if (_composeTestRule == null) composeTestRule.apply(base, description)
        else {
            object : Statement() {
                override fun evaluate() {
                    base.evaluate()
                }
            }
        }

    public fun setContent(
        creation: Creation,
        creationComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        onCoreDocumentCreated: ((CoreDocument) -> Unit)? = null,
        player: Player,
        size: Size,
        playComposableWrapper: (@Composable (composable: @Composable () -> Unit) -> Unit) = {
            it()
        },
        composable: @RemoteComposable @Composable () -> Unit,
    ) {
        composeTestRule.setContent {
            val coreDocumentState = remember { mutableStateOf<CoreDocument?>(null) }
            val creationContent: @Composable () -> Unit = {
                val docState = creation.rememberRemoteDocument(composable = composable)
                coreDocumentState.value = docState.value
            }
            creationComposableWrapper(creationContent)

            coreDocumentState.value?.let {
                onCoreDocumentCreated?.invoke(it)

                val composable: @Composable () -> Unit = {
                    player.Play(coreDocument = it, size = size)
                }
                playComposableWrapper { composable() }
            }
        }
    }

    public interface Creation {
        @Composable
        public fun rememberRemoteDocument(
            composable: @RemoteComposable @Composable () -> Unit
        ): MutableState<CoreDocument?>
    }

    public interface Player {
        @Composable public fun Play(coreDocument: CoreDocument, size: Size)
    }
}

/**
 * Captures the visual content of the root Compose node as an [ImageBitmap].
 *
 * @return The captured hierarchy rendering as an image.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteBaseContentTestRule.captureRootToImage(): ImageBitmap =
    this.composeTestRule.onRoot().captureToImage()

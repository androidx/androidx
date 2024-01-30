/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.foundation.LocalSwipeToDismissBackgroundScrimColor
import androidx.wear.compose.foundation.LocalSwipeToDismissContentScrimColor
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissKeys
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState

/**
 * Wear Material 3 [SwipeToDismissBox] that handles the swipe-to-dismiss gesture. Takes a single
 * slot for the background (only displayed during the swipe gesture) and the foreground content.
 *
 * Example of a [SwipeToDismissBox] with stateful composables:
 * @sample androidx.wear.compose.material3.samples.StatefulSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [SwipeToDismissBox]:
 * @sample androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * @param state State containing information about ongoing swipe or animation.
 * @param modifier [Modifier] for this component.
 * @param backgroundScrimColor [Color] for background scrim.
 * @param contentScrimColor [Color] used for the scrim over the content composable during
 * the swipe gesture.
 * @param backgroundKey [key] which identifies the content currently composed in
 * the [content] block when isBackground == true. Provide the backgroundKey if your background
 * content will be displayed as a foreground after the swipe animation ends
 * (as is common when [SwipeToDismissBox] is used for the navigation). This allows
 * remembered state to be correctly moved between background and foreground.
 * @param contentKey [key] which identifies the content currently composed in the
 * [content] block when isBackground == false. See [backgroundKey].
 * @param userSwipeEnabled Whether the swipe gesture is enabled.
 * (e.g. when there is no background screen, set userSwipeEnabled = false)
 * @param content Slot for content, with the isBackground parameter enabling content to be
 * displayed behind the foreground content - the background is normally hidden, is shown behind a
 * scrim during the swipe gesture, and is shown without scrim once the finger passes the
 * swipe-to-dismiss threshold.
 */
@Composable
public fun SwipeToDismissBox(
    state: SwipeToDismissBoxState,
    modifier: Modifier = Modifier,
    backgroundScrimColor: Color = MaterialTheme.colorScheme.background,
    contentScrimColor: Color = MaterialTheme.colorScheme.background,
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    userSwipeEnabled: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    CompositionLocalProvider(
        LocalSwipeToDismissBackgroundScrimColor provides backgroundScrimColor,
        LocalSwipeToDismissContentScrimColor provides contentScrimColor
    ) {
        androidx.wear.compose.foundation.SwipeToDismissBox(
            state = state,
            modifier = modifier,
            backgroundKey = backgroundKey,
            contentKey = contentKey,
            userSwipeEnabled = userSwipeEnabled,
            content = content
        )
    }
}

/**
 * Wear Material 3 [SwipeToDismissBox] that handles the swipe-to-dismiss gesture.
 * This overload takes an [onDismissed] parameter which is used to execute a command when the
 * swipe to dismiss has completed, such as navigating to another screen.
 *
 * Example of a simple SwipeToDismissBox:
 * @sample androidx.wear.compose.material3.samples.SimpleSwipeToDismissBox
 *
 * Example of using [Modifier.edgeSwipeToDismiss] with [SwipeToDismissBox]:
 * @sample androidx.wear.compose.material3.samples.EdgeSwipeForSwipeToDismiss
 *
 * For more information, see the
 * [Swipe to dismiss](https://developer.android.com/training/wearables/components/swipe-to-dismiss)
 * guide.
 *
 * @param onDismissed Executes when the swipe to dismiss has completed.
 * @param modifier [Modifier] for this component.
 * @param state State containing information about ongoing swipe or animation.
 * @param backgroundScrimColor [Color] for background scrim.
 * @param contentScrimColor [Color] used for the scrim over the content composable during
 * the swipe gesture.
 * @param backgroundKey [key] which identifies the content currently composed in
 * the [content] block when isBackground == true. Provide the backgroundKey if your background
 * content will be displayed as a foreground after the swipe animation ends
 * (as is common when [SwipeToDismissBox] is used for the navigation). This allows
 * remembered state to be correctly moved between background and foreground.
 * @param contentKey [key] which identifies the content currently composed in the
 * [content] block when isBackground == false. See [backgroundKey].
 * @param userSwipeEnabled Whether the swipe gesture is enabled.
 * (e.g. when there is no background screen, set userSwipeEnabled = false)
 * @param content Slot for content, with the isBackground parameter enabling content to be
 * displayed behind the foreground content - the background is normally hidden, is shown behind a
 * scrim during the swipe gesture, and is shown without scrim once the finger passes the
 * swipe-to-dismiss threshold.
 */
@Composable
public fun SwipeToDismissBox(
    onDismissed: () -> Unit,
    modifier: Modifier = Modifier,
    state: SwipeToDismissBoxState = rememberSwipeToDismissBoxState(),
    backgroundScrimColor: Color = MaterialTheme.colorScheme.background,
    contentScrimColor: Color = MaterialTheme.colorScheme.background,
    backgroundKey: Any = SwipeToDismissKeys.Background,
    contentKey: Any = SwipeToDismissKeys.Content,
    userSwipeEnabled: Boolean = true,
    content: @Composable BoxScope.(isBackground: Boolean) -> Unit
) {
    CompositionLocalProvider(
        LocalSwipeToDismissBackgroundScrimColor provides backgroundScrimColor,
        LocalSwipeToDismissContentScrimColor provides contentScrimColor
    ) {
        androidx.wear.compose.foundation.SwipeToDismissBox(
            state = state,
            modifier = modifier,
            onDismissed = onDismissed,
            backgroundKey = backgroundKey,
            contentKey = contentKey,
            userSwipeEnabled = userSwipeEnabled,
            content = content
        )
    }
}

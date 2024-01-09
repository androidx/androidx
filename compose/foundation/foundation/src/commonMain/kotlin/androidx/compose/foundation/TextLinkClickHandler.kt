/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.LinkAnnotation

/**
 * A handler that will be called when a user clicks a link from the text that is represented as a
 * [LinkAnnotation] annotation.
 *
 * If you need to make part of the text clickable and get notified when users clicks it, pass this
 * handler to the [BasicText] composable function.
 *
 * Note that if you pass this handler to the [BasicText], you will need to handle opening the url
 * manually. To do so use [androidx.compose.ui.platform.LocalUriHandler] composition local.
 *
 * @sample androidx.compose.foundation.samples.BasicTextWithTextLinkClickHandler
 */
@ExperimentalFoundationApi
@Stable
fun interface TextLinkClickHandler {

    /**
     * Called when a corresponding [link] is clicked by a user.
     */
    fun onClick(link: LinkAnnotation)
}

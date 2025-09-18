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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.frontend.capture

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RestrictTo

/**
 * This allows us to more easily resize the layout hosting the compose view, allowing us to control
 * the captured dimension for the composable
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ResizableLayout
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    private lateinit var remoteView: CaptureComposeView

    public fun use(remoteComposeView: CaptureComposeView) {
        addView(remoteComposeView)
        remoteView = remoteComposeView
    }
}

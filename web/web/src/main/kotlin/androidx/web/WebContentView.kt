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

package androidx.web

import android.content.Context
import android.util.AttributeSet
import android.webkit.WebView
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/**
 * This is a [WebView] that is created via [WebContent]. The underlying web state can outlive a
 * typical WebView.
 *
 * It is completely safe to instantiate this view as a fallback to traditional [WebView] usage even
 * when [WebFeature.WEB_CONTENT] feature checks fail.
 */
public open class WebContentView : WebView {
    public constructor(@NonNull context: Context) : super(context)

    public constructor(
        @NonNull context: Context,
        @Nullable attrs: AttributeSet?,
    ) : super(context, attrs)

    public constructor(
        @NonNull context: Context,
        @Nullable attrs: AttributeSet?,
        defStyleAttr: Int,
    ) : super(context, attrs, defStyleAttr)
}

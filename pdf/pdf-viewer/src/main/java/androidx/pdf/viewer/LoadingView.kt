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
package androidx.pdf.viewer

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.pdf.R

/**
 * A view which displays the loading spinner when the pdf is loading and displays an error messages
 * if there is a failure when rendering.
 */
// TODO(b/344386251): Update the error message and the text view once the mocks are finalised.
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class LoadingView : LinearLayout {
    private var progressBar: ProgressBar
    private var errorMessage: TextView

    public constructor(context: Context) : this(context, null)

    public constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0)

    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = 0
    ) : super(context, attrs, defStyleAttr) {

        // Inflate the layout
        LayoutInflater.from(context).inflate(R.layout.loading_view, this, true)

        // Find views
        progressBar = findViewById(R.id.loadingProgressBar)
        errorMessage = findViewById(R.id.errorTextView)
    }

    public fun showLoadingView() {
        progressBar.visibility = VISIBLE
        errorMessage.visibility = GONE
    }

    public fun showErrorView(message: String) {
        progressBar.visibility = GONE
        errorMessage.text = message
        errorMessage.visibility = VISIBLE
    }
}

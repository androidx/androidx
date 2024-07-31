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
import android.net.Uri
import android.view.View
import androidx.annotation.RestrictTo
import androidx.pdf.util.AnnotationUtils
import com.google.android.material.floatingactionbutton.FloatingActionButton

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class FabController(context: Context, uri: Uri, floatingActionButton: FloatingActionButton) {
    private var isAnnotationIntentResolvable: Boolean =
        AnnotationUtils.resolveAnnotationIntent(context, uri)

    public var isFabVisible: Boolean = floatingActionButton.visibility == View.VISIBLE
}

/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.appcompat.widget;

import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

class AppCompatHintHelper {

    static InputConnection onCreateInputConnection(InputConnection ic, EditorInfo outAttrs,
            View view) {
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and the parent implements WithHint, use its hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            ViewParent parent = view.getParent();
            while (parent instanceof View) {
                if (parent instanceof WithHint) {
                    outAttrs.hintText = ((WithHint) parent).getHint();
                    break;
                }
                parent = parent.getParent();
            }
        }
        return ic;
    }

    private AppCompatHintHelper() {
    }
}

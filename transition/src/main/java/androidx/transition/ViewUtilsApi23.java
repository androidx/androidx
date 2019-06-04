/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.transition;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(23)
class ViewUtilsApi23 extends ViewUtilsApi22 {

    /**
     * False when linking of the hidden setLeftTopRightBottom method has previously failed.
     */
    private static boolean sTryHiddenSetTransitionVisibility = true;

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public void setTransitionVisibility(@NonNull View view, int visibility) {
        // on P this method is blacklisted, so we have to resort to reflecting on mViewFlags
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
            super.setTransitionVisibility(view, visibility);
        } else {
            if (sTryHiddenSetTransitionVisibility) {
                // Since this was an @hide method made public, we can link directly against it with
                // a try/catch for its absence instead of doing the same through reflection.
                try {
                    view.setTransitionVisibility(visibility);
                } catch (NoSuchMethodError e) {
                    sTryHiddenSetTransitionVisibility = false;
                }
            }
        }
    }
}


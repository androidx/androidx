/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(19)
class ViewUtilsApi19 extends ViewUtilsBase {

    /**
     * False when linking of the hidden set[get]TransitionAlpha method has previously failed.
     */
    private static boolean sTryHiddenTransitionAlpha = true;

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public void setTransitionAlpha(@NonNull View view, float alpha) {
        if (sTryHiddenTransitionAlpha) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                view.setTransitionAlpha(alpha);
                return;
            } catch (NoSuchMethodError e) {
                sTryHiddenTransitionAlpha = false;
            }
        }
        view.setAlpha(alpha);
    }

    @Override
    @SuppressLint("NewApi") // Lint doesn't know about the hidden method.
    public float getTransitionAlpha(@NonNull View view) {
        if (sTryHiddenTransitionAlpha) {
            // Since this was an @hide method made public, we can link directly against it with
            // a try/catch for its absence instead of doing the same through reflection.
            try {
                return view.getTransitionAlpha();
            } catch (NoSuchMethodError e) {
                sTryHiddenTransitionAlpha = false;
            }
        }
        return view.getAlpha();
    }

    @Override
    public void saveNonTransitionAlpha(@NonNull View view) {
        // Do nothing
    }

    @Override
    public void clearNonTransitionAlpha(@NonNull View view) {
        // Do nothing
    }

}

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

package androidx.leanback.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatViewInflater;

/** Inflater that converts leanback non-AppCpmpat views in layout to AppCompat versions. */
public class LeanbackAppCompatViewInflater extends AppCompatViewInflater {

    @Override
    @NonNull
    protected View createView(@Nullable Context context, @Nullable String name,
            @Nullable AttributeSet attrs) {
        switch (name) {
            case "androidx.leanback.widget.GuidedActionEditText":
                return new GuidedActionAppCompatEditText(context, attrs);
        }
        return null;
    }

}

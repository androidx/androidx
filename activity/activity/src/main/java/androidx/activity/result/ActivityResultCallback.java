/*
 * Copyright (C) 2020 The Android Open Source Project
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


package androidx.activity.result;

import android.annotation.SuppressLint;
import android.app.Activity;

/**
 * A type-safe callback to be called when an {@link Activity#onActivityResult activity result}
 * is available.
 *
 * @param <O> result type
 */
public interface ActivityResultCallback<O> {

    /**
     * Called when result is available
     */
    void onActivityResult(@SuppressLint("UnknownNullness") O result);
}

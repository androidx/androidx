/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.viewpager2.adapter;

import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;

/**
 * {@link ViewPager2} adapters should implement this interface to be called during
 * {@link View#onSaveInstanceState()} and {@link View#onRestoreInstanceState(Parcelable)}
 */
public interface StatefulAdapter {
    /** Saves adapter state */
    @NonNull Parcelable saveState();

    /** Restores adapter state */
    void restoreState(@NonNull Parcelable savedState);
}

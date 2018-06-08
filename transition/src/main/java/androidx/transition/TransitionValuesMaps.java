/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.util.SparseArray;
import android.view.View;

import androidx.collection.ArrayMap;
import androidx.collection.LongSparseArray;

class TransitionValuesMaps {

    final ArrayMap<View, TransitionValues> mViewValues = new ArrayMap<>();

    final SparseArray<View> mIdValues = new SparseArray<>();

    final LongSparseArray<View> mItemIdValues = new LongSparseArray<>();

    final ArrayMap<String, View> mNameValues = new ArrayMap<>();

}

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

package com.android.flatfoot.apireviewdemo.lifecycle_04_shared_viewmodel;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.graphics.drawable.ShapeDrawable;

public class SharedViewModel extends ViewModel {
    public final MutableLiveData<ShapeDrawable> shapeDrawableData = createEmpty();

    private static MutableLiveData<ShapeDrawable> createEmpty() {
        MutableLiveData<ShapeDrawable> empty = new MutableLiveData<>();
        empty.setValue(null);
        return empty;
    }
}

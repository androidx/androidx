/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.view;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

/**
 * A {@link LiveData} whose value is based on a source {@link LiveData} of the same type.
 *
 * <p> Used by {@link CameraController} to get a camera state {@link LiveData} before camera is
 * bound, and keep using the same {@link LiveData} after camera is switched.
 *
 * <p> Setting a new source will remove the previous source.
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
final class ForwardingLiveData<T> extends MediatorLiveData<T> {

    private LiveData<T> mLiveDataSource;

    void setSource(@NonNull LiveData<T> liveDataSource) {
        if (mLiveDataSource != null) {
            super.removeSource(mLiveDataSource);
        }
        mLiveDataSource = liveDataSource;
        super.addSource(liveDataSource, this::setValue);
    }

    @Override
    public T getValue() {
        // If MediatorLiveData has no active observers, it will not receive updates
        // when the source is updated, in which case the value of this class and its source
        // will be out-of-sync.
        // We need to retrieve the source value for the caller.
        return mLiveDataSource == null ? null : mLiveDataSource.getValue();
    }
}

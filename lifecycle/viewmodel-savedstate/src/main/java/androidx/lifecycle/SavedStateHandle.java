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

package androidx.lifecycle;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A handle to saved state passed down to {@link ViewModel}
 */
public class SavedStateHandle {
    private final Bundle mInitialArgs;
    private final SavedStateAccessorHolder mHolder;

    /**
     * Creates a handle with the given initial arguments.
     */
    public SavedStateHandle(@Nullable Bundle initialArgs, @Nullable Bundle savedState) {
        mInitialArgs = initialArgs;
        mHolder = new SavedStateAccessorHolder(savedState);
    }

    /**
     * Returns {@link SavedStateAccessor} that allows to read and write saved state
     * associate with a ViewModel
     */
    @NonNull
    public SavedStateAccessor accessor() {
        return mHolder.savedStateAccessor();
    }

    SavedStateRegistry.SavedStateProvider<Bundle> savedStateComponent() {
        return mHolder;
    }

    /**
     * Arguments of the owning component.
     */
    @Nullable
    public Bundle initialArguments() {
        return mInitialArgs;
    }
}

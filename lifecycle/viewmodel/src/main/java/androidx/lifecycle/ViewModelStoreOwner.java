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

package androidx.lifecycle;

import androidx.annotation.NonNull;

/**
 * A scope that owns {@link ViewModelStore}.
 * <p>
 * A responsibility of an implementation of this interface is to retain owned ViewModelStore
 * during the configuration changes and call {@link ViewModelStore#clear()}, when this scope is
 * going to be destroyed.
 */
@SuppressWarnings("WeakerAccess")
public interface ViewModelStoreOwner {
    /**
     * Returns owned {@link ViewModelStore}
     *
     * @return a {@code ViewModelStore}
     */
    @NonNull
    ViewModelStore getViewModelStore();
}

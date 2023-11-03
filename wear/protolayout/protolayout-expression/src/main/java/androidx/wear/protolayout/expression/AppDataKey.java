/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.protolayout.expression;

import androidx.annotation.NonNull;

/**
 * Represent a {@link DynamicDataKey} that references app/tile pushed state data.
 *
 * @param <T> The data type of the dynamic values that this key is bound to.
 */
public final class AppDataKey<T extends DynamicBuilders.DynamicType> extends DynamicDataKey<T> {
    @NonNull private static final String DEFAULT_NAMESPACE = "";

    /**
     * Create a {@link AppDataKey} with the specified key.
     *
     * @param key The key in the state to bind to.
     */
    public AppDataKey(@NonNull String key) {
        super(DEFAULT_NAMESPACE, key);
    }
}

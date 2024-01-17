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

package androidx.wear.protolayout.expression.pipeline;

import androidx.annotation.NonNull;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.PlatformDataKey;

import java.util.Set;

/** Callback for receiving a PlatformDataProvider's new data. */
public interface PlatformDataReceiver {

    /**
     * Called by the registered {@link PlatformDataProvider} to send new values.
     *
     * @param newData The new values for the registered keys.
     */
    void onData(@NonNull PlatformDataValues newData);

    /**
     * Called by the registered {@link PlatformDataProvider} to notify that the current data has
     * been invalidated. Typically, this invalidated status is transient and subsequent onData call
     * can be followed to sent new values.
     *
     * @param keys The set of keys with current data been invalidated.
     */
    void onInvalidated(@NonNull Set<PlatformDataKey<?>> keys);
}

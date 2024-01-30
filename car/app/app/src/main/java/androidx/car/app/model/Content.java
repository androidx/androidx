/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.car.app.model;

import androidx.annotation.NonNull;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;

/** Interface implemented by models that can be invalidated and refreshed individually. */
@CarProtocol
@RequiresCarApi(6)
public interface Content {

    /**
     * Returns the associated content ID for a component. This ID will be used to trigger
     * invalidation and refresh of the component, and can not change.
     *
     */
    @NonNull
    String getContentId();
}


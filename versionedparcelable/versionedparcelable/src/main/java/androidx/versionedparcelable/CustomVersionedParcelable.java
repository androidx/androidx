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

package androidx.versionedparcelable;

import androidx.annotation.RestrictTo;

/**
 * A VersionedParcelable that gets callbacks right before serialization
 * and right after deserialization to handle custom fields.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class CustomVersionedParcelable implements VersionedParcelable {

    /**
     * Called immediately before this object is going to be serialized, can be used
     * to handle any custom fields that cannot be easily annotated.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public void onPreParceling(boolean isStream) {
    }

    /**
     * Called immediately after this object has been deserialized, can be used
     * to handle any custom fields that cannot be easily annotated.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public void onPostParceling() {
    }
}

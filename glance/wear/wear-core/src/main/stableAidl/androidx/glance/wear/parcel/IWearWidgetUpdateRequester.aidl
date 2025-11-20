/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.glance.wear.parcel;

import androidx.glance.wear.parcel.IExecutionCallback;
import androidx.glance.wear.parcel.WearWidgetRawContentParcel;

/**
  * Interface, implemented by Widget renderers, which allows Widget Providers to push updates.
  */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IWearWidgetUpdateRequester {
    const int API_VERSION = 1;

    // TODO: b/451989641 - define other update error codes here.
    const int UPDATE_ERROR_CODE_INTERNAL_ERROR = 1;

    /**
      * Gets the version of this interface implemented by this service.
      *
      * @since version 1
      */
    int getApiVersion() = 0;

    /**
      * Request that the Widget Renderer updates the Widget with the given contents.
      *
      * {@param instanceId} the instance id of the widget being updated. The Widget with the
      *   specified {@code instanceId} must be owned by the calling package.
      * {@param contentParcel} contains the Widget contents to be used in the update.
      * {@param callback} called when this request update succeeds or fails.
      *
      * @since version 1
      */
    oneway void requestUpdate(
        in int instanceId,
        in WearWidgetRawContentParcel contentParcel,
        in IExecutionCallback callback
    ) = 1;
}

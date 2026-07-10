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

import androidx.glance.wear.parcel.ActiveWearWidgetHandleParcel;
import androidx.glance.wear.parcel.IExecutionCallback;
import androidx.glance.wear.parcel.IWearWidgetCallback;
import androidx.glance.wear.parcel.WearWidgetRequestParcel;
import androidx.glance.wear.parcel.WearWidgetEventBatchParcel;

/**
  * Interface to be implemented by a service which provides Widgets on a Wear
  * device.
  *
  */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IWearWidgetProvider {
    const int API_VERSION = 1;

    const int ERROR_CODE_INTERNAL_ERROR = 1;
    const int ERROR_CODE_INVALID_ARGUMENT = 2;

    /**
      * Gets the version of this WearWidgetProvider interface implemented by
      * this service.
      *
      * @since version 1
      */
    int getApiVersion() = 0;

    /**
      * Called when the system requests a Widget update from this provider.
      * The provider should call callback.updateWidgetContent to provide the update.
      *
      * @since version 1
      */
    oneway void onWidgetRequest(in WearWidgetRequestParcel requestParcel, IWearWidgetCallback callback) = 1;

    /**
     * Called when the widget becomes active in the Host.
     *
     * There are currently no Hosts that send this event.
     *
     * @since version 1
     */
    oneway void onActivated(in ActiveWearWidgetHandleParcel handleParcel, IExecutionCallback callback) = 2;

    /**
     * Called when the widget becomes de-activated in the Host.
     *
     * There are currently no Hosts that send this event.
     *
     * @since version 1
     */
    oneway void onDeactivated(in ActiveWearWidgetHandleParcel handleParcel, IExecutionCallback callback) = 3;

    /**
     * Called when the widget is added to the Host.
     *
     * @since version 1
     */
    oneway void onAdded(in ActiveWearWidgetHandleParcel handleParcel, IExecutionCallback callback) = 4;

    /**
     * Called when the widget is removed from the Host.
     *
     * @since version 1
     */
    oneway void onRemoved(in ActiveWearWidgetHandleParcel handleParcel, IExecutionCallback callback) = 5;

    /**
     * Called periodically when the Host sends batched interaction events.
     *
     * @since version 1
     */
    oneway void onEvents(in WearWidgetEventBatchParcel eventBatchParcel, IExecutionCallback callback) = 6;
}

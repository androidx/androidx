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

import androidx.glance.wear.parcel.WearWidgetRawContentParcel;

/**
  * Callback used to return widget contents from a Provider to the system.
  */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IWearWidgetCallback {

    /**
      * Called when the system requests a Widget update from this provider.
      * The provider should call callback.updateWidgetContent to provide the update.
      *
      * @since version 1
      */
    oneway void updateWidgetContent(in WearWidgetRawContentParcel contentParcel) = 0;
}

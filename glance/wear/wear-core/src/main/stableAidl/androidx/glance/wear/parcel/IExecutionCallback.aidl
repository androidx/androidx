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

/**
  * Callback used to inform the host that an execution has completed.
  */
@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IExecutionCallback {

    /**
      * Called to inform another process that the execution has completed successfully.
      */
    oneway void onSuccess() = 0;

    /**
      * Called to inform another process that the execution has failed.
      *
      * <p>The error codes should be defined by each site using this callback.
      */
    oneway void onError(in int errorCode, in String message) = 1;
}

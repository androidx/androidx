/**
 * Copyright 2021, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.core.app.unusedapprestrictions;

import androidx.core.app.unusedapprestrictions.IUnusedAppRestrictionsBackportCallback;

@JavaPassthrough(annotation="@androidx.annotation.RestrictTo(androidx.annotation.RestrictTo.Scope.LIBRARY)")
interface IUnusedAppRestrictionsBackportService {

  /**
   * Checks whether permission revocation is enabled for the calling application.
   *
   * <p>This API is only intended to work for the backported version of
   * permission revocation running on Android M-Q and will not work for Android
   * R+ versions of permission revocation. Only the Verifier on the device can implement this,
   * as that is the component responsible for auto-revoking permissions on M-Q devices.
   *
   * @param callback An IUnusedAppRestrictionsBackportCallback object that will
   * be called with the results of this API
   */
  oneway void isPermissionRevocationEnabledForApp(in IUnusedAppRestrictionsBackportCallback callback);
}

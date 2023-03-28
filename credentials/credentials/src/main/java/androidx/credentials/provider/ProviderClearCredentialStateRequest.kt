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

package androidx.credentials.provider

import android.service.credentials.CallingAppInfo

/**
 * Request class for clearing a user's credential state from the credential providers.
 *
 * @property callingAppInfo info pertaining to the calling app that's making the request
 */
class ProviderClearCredentialStateRequest constructor(val callingAppInfo: CallingAppInfo)
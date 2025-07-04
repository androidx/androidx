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

package androidx.credentials

import androidx.annotation.RestrictTo

/**
 * Received by calling apps on successful propagation of a signal credential state request to the
 * user's credential providers.
 *
 * This is an empty response and simply indicates the signal request has been successfully passed to
 * the providers.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY) class SignalCredentialStateResponse {}

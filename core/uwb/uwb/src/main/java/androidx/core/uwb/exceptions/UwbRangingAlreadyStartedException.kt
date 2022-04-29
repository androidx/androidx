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

package androidx.core.uwb.exceptions

/**
 * The ranging has already started for the [UwbClientSessionScope], but still received another
 * request to start ranging. You cannot reuse a [UwbClientSessionScope] for multiple ranging
 * sessions. To have multiple consumers for a single ranging session, use a [SharedFlow].
 */
class UwbRangingAlreadyStartedException(message: String) : UwbApiException(message)
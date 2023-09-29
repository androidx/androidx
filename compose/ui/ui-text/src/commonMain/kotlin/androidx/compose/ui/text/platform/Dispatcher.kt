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

package androidx.compose.ui.text.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Dispatcher to do font load cache coroutines on during font load fallback.
 *
 * Very little actual work happens on this dispatcher, and it does not need to spin up a thread for
 * this work.
 *
 * This should be Main or some other always-available dispatcher.
 *
 * It is important that this dispatcher, if it is shared with other coroutines,  implements `yield`
 * to dispatch.
 */
internal val FontCacheManagementDispatcher: CoroutineDispatcher = Dispatchers.Main

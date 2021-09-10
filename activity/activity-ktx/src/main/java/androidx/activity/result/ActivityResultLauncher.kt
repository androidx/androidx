/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.result

import androidx.core.app.ActivityOptionsCompat

/**
 * Convenience method to launch a no-argument registered call without needing to pass in `null`.
 */
public fun ActivityResultLauncher<Void?>.launch(options: ActivityOptionsCompat? = null) {
    launch(null, options)
}

/**
 * Convenience method to launch a no-argument registered call without needing to pass in `Unit`.
 */
@JvmName("launchUnit")
public fun ActivityResultLauncher<Unit>.launch(options: ActivityOptionsCompat? = null) {
    launch(Unit, options)
}
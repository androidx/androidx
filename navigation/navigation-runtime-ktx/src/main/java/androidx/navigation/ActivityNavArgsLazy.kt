/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation

import android.app.Activity
import androidx.annotation.MainThread

/**
 * Returns a [Lazy] delegate to access the Activity's extras as an [Args] instance.
 *
 * It is strongly recommended that this method only be used when the Activity is started
 * by [androidx.navigation.NavController.navigate] with the corresponding
 * [androidx.navigation.NavDirections] object, which ensures that the required
 * arguments are present.
 *
 * ```
 * class MyActivity : Activity() {
 *     val args: MyActivityArgs by navArgs()
 * }
 * ```
 *
 * This property can be accessed only after the Activity is attached to the Application,
 * and access prior to that will result in IllegalStateException.
 */
@MainThread
public inline fun <reified Args : NavArgs> Activity.navArgs(): NavArgsLazy<Args> =
    NavArgsLazy(Args::class) {
        intent?.let { intent ->
            intent.extras
                ?: throw IllegalStateException("Activity $this has null extras in $intent")
        } ?: throw IllegalStateException("Activity $this has a null Intent")
    }

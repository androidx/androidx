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

@file:JvmName("NavBackStackEntryKt")
@file:JvmMultifileClass

package androidx.navigation

import android.app.Application
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigation.internal.NavContext
import java.util.UUID

internal actual fun randomUuid(): String = UUID.randomUUID().toString()

internal actual fun MutableCreationExtras.setPlatformExtras(context: NavContext?) {
    (context?.getApplication() as? Application)?.let { application ->
        this[APPLICATION_KEY] = application
    }
}

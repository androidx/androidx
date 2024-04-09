/*
 * Copyright (C) 2017 The Android Open Source Project
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
package androidx.lifecycle

import android.app.Application

/**
 * Application context aware [ViewModel].
 *
 * Subclasses must have a constructor which accepts [Application] as the only parameter.
 */
public open class AndroidViewModel(private val application: Application) : ViewModel() {

    /**
     * Return the application.
     */
    @Suppress("UNCHECKED_CAST")
    public open fun <T : Application> getApplication(): T {
        return application as T
    }
}

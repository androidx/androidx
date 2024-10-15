/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.autofill

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * Autofill API.
 *
 * This interface is available to all composables via a CompositionLocal. The composable can then
 * notify the Autofill framework that user values have been committed as required.
 */
@ExperimentalComposeUiApi
interface AutofillManager {

    /**
     * Notify credentials have been committed.
     *
     * This function is called when the Autofill framework needs to be notified that credentials
     * have been entered by the user.
     */
    fun commit()
}

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

/**
 * Autofill API.
 *
 * This interface is available to all composables via a CompositionLocal. The composable can then
 * notify the Autofill framework that user values have been committed as required.
 */
expect class AutofillManager {

    /**
     * Indicate the autofill context should be committed.
     *
     * Call this function to notify the Autofill framework that the current context should be
     * committed. After calling this function, the framework considers the form submitted, and the
     * credentials entered will be processed.
     */
    fun commit()

    /**
     * Indicate the autofill context should be canceled.
     *
     * Call this function to notify the Autofill framework that the current context should be
     * canceled. After calling this function, the framework will stop the current autofill session
     * without processing any information entered in the autofillable field.
     */
    fun cancel()
}

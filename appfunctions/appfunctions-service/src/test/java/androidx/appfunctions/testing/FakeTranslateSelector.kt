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

package androidx.appfunctions.testing

import androidx.appfunctions.internal.Translator
import androidx.appfunctions.internal.TranslatorSelector
import androidx.appfunctions.metadata.AppFunctionSchemaMetadata

internal class FakeTranslatorSelector : TranslatorSelector {
    private var translator: Translator? = null

    override fun getTranslator(schemaMetadata: AppFunctionSchemaMetadata): Translator? = translator

    fun setTranslator(translator: Translator) {
        this.translator = translator
    }
}

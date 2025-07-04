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

package androidx.pdf.view

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.pdf.updateContext
import java.util.Locale

/** Bare bones test helper [Activity] for [PdfView] integration tests */
open class PdfViewTestActivity : Activity() {

    override fun attachBaseContext(newBase: Context?) {
        // Update context of test activity if custom locale is injected
        if (
            newBase != null &&
                intent != null &&
                intent.hasExtra(LOCALE_LANGUAGE) &&
                intent.hasExtra(LOCALE_COUNTRY)
        ) {
            val language = intent.getStringExtra(LOCALE_LANGUAGE) as String
            val country = intent.getStringExtra(LOCALE_COUNTRY) as String
            @Suppress("Deprecation") val newLocale = Locale(language, country)
            val wrappedContext = updateContext(newBase, newLocale)
            return super.attachBaseContext(wrappedContext)
        }
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onCreateCallback(this)
        // disable enter animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }

    override fun finish() {
        super.finish()
        // disable exit animation.
        @Suppress("Deprecation") overridePendingTransition(0, 0)
    }

    companion object {
        var onCreateCallback: ((PdfViewTestActivity) -> Unit) = {}

        const val LOCALE_LANGUAGE = "language"
        const val LOCALE_COUNTRY = "country"
    }
}

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

package androidx.appcompat.app

import android.os.Bundle

import androidx.appcompat.test.R

class NoThemeAppCompatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // We don't have an AppCompat theme by default, but this
        // shouldn't create the subDecor - it should just no-op
        invalidateOptionsMenu()

        super.onCreate(savedInstanceState)

        // Now set the theme to whatever AppCompat theme we have in tests
        setTheme(R.style.Theme_TextColors)

        // And call setContentView(), which will internally create the subDecor
        setContentView(R.layout.layout_actv)
    }
}

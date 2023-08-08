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

package androidx.fragment.app.testing

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.fragment.app.FragmentActivity
import androidx.fragment.testing.manifest.R

/**
 * An empty activity inheriting FragmentActivity. This Activity is used to host Fragment in
 * FragmentScenario.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class EmptyFragmentActivity : FragmentActivity() {
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(
            intent.getIntExtra(
                THEME_EXTRAS_BUNDLE_KEY,
                R.style.FragmentScenarioEmptyFragmentActivityTheme
            )
        )

        // Checks if we have a custom FragmentFactory and set it.
        val factory = FragmentFactoryHolderViewModel.getInstance(this).fragmentFactory
        if (factory != null) {
            supportFragmentManager.fragmentFactory = factory
        }

        // FragmentFactory needs to be set before calling the super.onCreate, otherwise the
        // Activity crashes when it is recreating and there is a fragment which has no
        // default constructor.
        super.onCreate(savedInstanceState)
    }

    companion object {
        const val THEME_EXTRAS_BUNDLE_KEY = "androidx.fragment.app.testing.FragmentScenario" +
            ".EmptyFragmentActivity.THEME_EXTRAS_BUNDLE_KEY"
    }
}

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

package androidx.activity.lint.stubs

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.java
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin

private val ACTIVITY_RESULT_CALLER =
    java(
        """
    package androidx.activity.result;

    public class ActivityResultCaller {
        public ActivityResultLauncher registerForActivityResult(ActivityResultContract contract) {}
    }
"""
    )

private val ACTIVITY_RESULT_CONTRACT =
    java(
        """
    package androidx.activity.result.contract;

    public class ActivityResultContract { }
"""
    )

private val COMPONENT_ACTIVITY =
    kotlin(
        """
    package androidx.activity

    class ComponentActivity {
        open fun onBackPressed() { }
    }
"""
    )

private val ON_BACK_PRESSED_CALLBACK =
    kotlin(
        """
    package androidx.activity

    class OnBackPressedCallback {
        open fun handleOnBackPressed() { }
    }
"""
    )

private val ON_BACK_PRESSED_DISPATCHER =
    kotlin(
        """
    package androidx.activity

    class OnBackPressedDispatcher {
        open fun onBackPressed() { }
    }
"""
    )

// stubs for testing calls to registerForActivityResult
internal val STUBS =
    arrayOf(
        ACTIVITY_RESULT_CALLER,
        ACTIVITY_RESULT_CONTRACT,
        COMPONENT_ACTIVITY,
        ON_BACK_PRESSED_CALLBACK,
        ON_BACK_PRESSED_DISPATCHER
    )

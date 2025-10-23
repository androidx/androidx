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

package androidx.compose.ui.util.mockito

import com.android.dx.mockito.DexmakerMockMaker
import com.android.dx.mockito.inline.DexmakerStackTraceCleaner
import org.mockito.exceptions.stacktrace.StackTraceCleaner
import org.mockito.internal.exceptions.stacktrace.DefaultStackTraceCleaner

/**
 * Similar to [CustomMockMaker], delegates the stack cleaner logic to mix dexmaker-mockito and
 * dexmaker-mockito-inline.
 *
 * This class was originally forked from the CustomStackTraceCleaner in androidx core (util).
 */
class CustomStackTraceCleaner : StackTraceCleaner {

    companion object {
        private val CLEANER_WRAPPER =
            DefaultStackTraceCleaner()
                .let { DexmakerMockMaker().getStackTraceCleaner(it) }
                .let { DexmakerStackTraceCleaner().getStackTraceCleaner(it) }
    }

    override fun isIn(candidate: StackTraceElement?) = CLEANER_WRAPPER.isIn(candidate)
}

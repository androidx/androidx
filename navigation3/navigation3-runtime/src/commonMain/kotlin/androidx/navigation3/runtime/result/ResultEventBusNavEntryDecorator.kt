/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.navigation3.runtime.result

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavEntryDecorator

/** Returns a [ResultEventBusNavEntryDecorator] that is remembered across recompositions. */
@Composable
public fun <T : Any> rememberResultEventBusNavEntryDecorator(): ResultEventBusNavEntryDecorator<T> =
    remember {
        ResultEventBusNavEntryDecorator()
    }

/**
 * Wraps the content of a [NavEntry] with a [LocalResultEventBus] to provide the ability to pass
 * results to previous entries on the navigation backstack.
 */
public class ResultEventBusNavEntryDecorator<T : Any>(
    private val bus: ResultEventBus = ResultEventBus()
) :
    NavEntryDecorator<T>(
        onPop = {},
        decorate = { entry ->
            CompositionLocalProvider(LocalResultEventBus provides bus) { entry.Content() }
        },
    )

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

package androidx.navigation3

import androidx.collection.MutableObjectIntMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Wraps the content of a [NavEntry] with a [SaveableStateHolder.SaveableStateProvider] to ensure
 * that calls to [rememberSaveable] within the content work properly and that state can be saved.
 *
 * This [NavLocalProvider] is the only one that is **required** as saving state is considered a
 * non-optional feature.
 */
public class SaveableStateNavLocalProvider : NavLocalProvider {

    @Composable
    override fun ProvideToBackStack(backStack: List<Any>, content: @Composable () -> Unit) {
        val localInfo = remember { SaveableStateNavLocalInfo() }
        DisposableEffect(key1 = backStack) {
            localInfo.refCount.clear()
            onDispose {}
        }

        localInfo.savedStateHolder = rememberSaveableStateHolder()
        backStack.forEach { key ->
            // We update here as part of composition to ensure the value is available to
            // ProvideToEntry
            localInfo.refCount[key] = backStack.count { it == key }
            DisposableEffect(key1 = key) {
                // We update here at the end of composition in case the backstack changed and
                // everything was cleared.
                localInfo.refCount[key] = backStack.count { it == key }
                onDispose {
                    // If the backStack count is less than the refCount for the key, remove the
                    // state since that means we removed a key from the backstack, and set the
                    // refCount to the backstack count.
                    val backstackCount = backStack.count { it == key }
                    if (backstackCount < localInfo.refCount[key]) {
                        localInfo.savedStateHolder!!.removeState(
                            getIdForKey(key, localInfo.refCount[key])
                        )
                        localInfo.refCount[key] = backstackCount
                    }
                    // If the refCount is 0, remove the key from the refCount.
                    if (localInfo.refCount[key] == 0) {
                        localInfo.refCount.remove(key)
                    }
                }
            }
        }

        CompositionLocalProvider(LocalSaveableStateNavLocalInfo provides localInfo) {
            content.invoke()
        }
    }

    @Composable
    public override fun <T : Any> ProvideToEntry(entry: NavEntry<T>) {
        val localInfo = LocalSaveableStateNavLocalInfo.current
        val key = entry.key
        val refCount = localInfo.refCount[key]
        val id: Int = rememberSaveable(key, refCount) { getIdForKey(key, refCount) }
        localInfo.savedStateHolder?.SaveableStateProvider(id) { entry.content.invoke(key) }
    }
}

internal val LocalSaveableStateNavLocalInfo =
    staticCompositionLocalOf<SaveableStateNavLocalInfo> {
        error(
            "CompositionLocal LocalSaveableStateNavLocalInfo not present. You must call " +
                "ProvideToBackStack before calling ProvideToEntry."
        )
    }

internal class SaveableStateNavLocalInfo {
    internal var savedStateHolder: SaveableStateHolder? = null
    internal val refCount: MutableObjectIntMap<Any> = MutableObjectIntMap()
}

internal fun getIdForKey(key: Any, count: Int): Int = 31 * key.hashCode() + count

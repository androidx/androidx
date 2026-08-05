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

@file:Suppress("UNCHECKED_CAST")

package androidx.compose.ui.platform

import android.os.Binder
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import android.view.View
import androidx.collection.MutableScatterMap
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.snapshots.SnapshotMutableState
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.R
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.read
import androidx.savedstate.savedState
import java.io.Serializable

private const val KEY = "androidx.compose.ui.platform.DisposableSaveableStateRegistry"

/** Creates [DisposableSaveableStateRegistry] associated with these [view] and [owner]. */
internal fun DisposableSaveableStateRegistry(
    view: View,
    owner: SavedStateRegistryOwner,
): DisposableSaveableStateRegistry {
    // The view id of AbstractComposeView is used as a key for SavedStateRegistryOwner. If there
    // are multiple AbstractComposeViews in the same Activity/Fragment with the same id(or with
    // no id) this means only the first view will restore its state. There is also an internal
    // mechanism to provide such id not as an Int to avoid ids collisions via view's tag. This
    // api is currently internal to compose:ui, we will see in the future if we need to make a
    // new public api for that use case.
    val composeView = (view.parent as View)
    val idFromTag = composeView.getTag(R.id.compose_view_saveable_id_tag) as? String
    val id = idFromTag ?: composeView.id.toString()
    return DisposableSaveableStateRegistry(id, owner)
}

/**
 * Creates [DisposableSaveableStateRegistry] with the restored values using [SavedStateRegistry] and
 * saves the values when [SavedStateRegistry] performs save.
 *
 * To provide a namespace we require unique [id]. We can't use the default way of doing it when we
 * have unique id on [AbstractComposeView] because we dynamically create [AbstractComposeView]s and
 * there is no way to have a unique id given there are could be any number of [AbstractComposeView]s
 * inside the same Activity. If we use [View.generateViewId] this id will not survive Activity
 * recreation. But it is reasonable to ask our users to have an unique id on [AbstractComposeView].
 */
internal fun DisposableSaveableStateRegistry(
    id: String,
    savedStateRegistryOwner: SavedStateRegistryOwner,
): DisposableSaveableStateRegistry {
    val key = "SaveableStateRegistry:$id"

    val androidxRegistry = savedStateRegistryOwner.savedStateRegistry
    val bundle = androidxRegistry.consumeRestoredStateForKey(key)
    val restored: Map<String, List<Any?>>? = bundle?.toMap()

    val saveableStateRegistry = SaveableStateRegistry(restored) { canBeSavedToBundle(it) }
    val registered =
        if (androidxRegistry.getSavedStateProvider(key) != null) {
            // Another View already has the same key, so the provider can't be registered. If
            // registerSavedStateProvider is called, it will throw IllegalArgumentException and
            // that is slower than checking in advance.
            false
        } else {
            try {
                androidxRegistry.registerSavedStateProvider(key) {
                    saveableStateRegistry.performSave().toBundle()
                }
                true
            } catch (_: IllegalArgumentException) {
                // this means there are two AndroidComposeViews composed into different parents with
                // the
                // same view id. currently we will just not save/restore state for the second
                // AndroidComposeView.
                // TODO: we should verify our strategy for such cases and improve it. b/162397322
                false
            }
        }
    return DisposableSaveableStateRegistry(saveableStateRegistry) {
        if (registered) {
            androidxRegistry.unregisterSavedStateProvider(key)
        }
    }
}

/** [SaveableStateRegistry] which can be disposed using [dispose]. */
internal class DisposableSaveableStateRegistry(
    saveableStateRegistry: SaveableStateRegistry,
    private val onDispose: () -> Unit,
) : SaveableStateRegistry by saveableStateRegistry {

    fun dispose() {
        onDispose()
    }
}

/**
 * Checks if [value] can be stored in a [Bundle].
 *
 * **IMPORTANT:** Uses direct type checks instead of reflection to avoid class check overhead at
 * runtime.
 */
private fun canBeSavedToBundle(value: Any): Boolean {
    // SnapshotMutableStateImpl is Parcelable, but inner state value might not be saveable.
    if (value is SnapshotMutableState<*>) {
        // Custom policies are not serializable.
        val policy = value.policy
        if (
            policy !== neverEqualPolicy<Any?>() &&
                policy !== structuralEqualityPolicy<Any?>() &&
                policy !== referentialEqualityPolicy<Any?>()
        ) {
            return false
        }

        // Must check if inner value is serializable.
        val stateValue = value.value
        return stateValue == null || canBeSavedToBundle(stateValue)
    }

    // Lambdas implement Serializable but crash on save. Check both Function and
    // Serializable to support custom classes implementing Function.
    if (value is Function<*> && value is Serializable) {
        return false
    }

    // Check interface. String is Serializable.
    if (value is Parcelable || value is Serializable) {
        return true
    }

    // Check other Bundle supported types. Do not implement Parcelable or Serializable.
    @Suppress("USELESS_IS_CHECK") // SizeF is not Parcelable before API 31.
    if (value is Binder || value is Size || value is SizeF || value is SparseArray<*>) {
        return true
    }

    return false
}

@Suppress("DEPRECATION")
private fun Bundle.toMap(): Map<String, List<Any?>> {
    return read { getParcelableOrNull<ParcelableMapHolder>(KEY)?.map } ?: emptyMap()
}

private fun Map<String, List<Any?>>.toBundle(): Bundle {
    return savedState { putParcelable(KEY, ParcelableMapHolder(this@toBundle)) }
}

/**
 * Holder wrapping [SaveableStateRegistry] state [Map].
 *
 * **Rationale**: During configuration changes, [ParcelableMapHolder] is passed by reference in
 * memory, avoiding collection copies. During process death, it serializes state using custom
 * parceling.
 *
 * Class must not implement [Map] interface. Android OS `Parcel.writeValue` matches [Map] interface
 * before `Serializable` or `Parcelable`. If class implements [Map], `writeValue` serializes it as
 * standard JVM `HashMap` (via optimized `writeMapInternal`), bypassing custom `Parcelable`
 * implementations. Holding [map] reference prevents matching and preserves custom parceling.
 */
@Suppress("AsCollectionCall", "BanParcelableUsage")
internal class ParcelableMapHolder(val map: Map<String, List<Any?>>) : Parcelable {

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(map.size)
        map.forEach { (key, value) ->
            parcel.writeString(key)
            parcel.writeValue(value)
        }
    }

    override fun describeContents(): Int = 0

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ParcelableMapHolder> =
            object : Parcelable.ClassLoaderCreator<ParcelableMapHolder> {
                override fun createFromParcel(
                    parcel: Parcel,
                    loader: ClassLoader?,
                ): ParcelableMapHolder {
                    val classLoader = loader ?: ParcelableMapHolder::class.java.classLoader
                    val size = parcel.readInt()
                    val map = MutableScatterMap<String, List<Any?>>(initialCapacity = size)
                    for (i in 0 until size) {
                        val key = parcel.readString() ?: continue
                        val value = parcel.readValue(classLoader) as List<Any?>
                        map[key] = value
                    }
                    return ParcelableMapHolder(map.asMap())
                }

                override fun createFromParcel(parcel: Parcel): ParcelableMapHolder {
                    return createFromParcel(parcel, loader = null)
                }

                override fun newArray(size: Int): Array<ParcelableMapHolder?> {
                    return arrayOfNulls(size)
                }
            }
    }
}

/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.lifecycle

/**
 * Returns a [LiveData] mapped from `this` LiveData by applying [transform] to each value set on
 * `this` LiveData.
 *
 * This method is analogous to [io.reactivex.Observable.map].
 *
 * [transform] will be executed on the main thread.
 *
 * Here is an example mapping a simple `User` struct in a `LiveData` to a
 * `LiveData` containing their full name as a `String`.
 *
 * ```
 * val userLD : LiveData<User> = ...;
 * val userFullNameLD: LiveData<String> = userLD.map { user -> user.firstName + user.lastName }
 * ```
 */
inline fun <X, Y> LiveData<X>.map(crossinline transform: (X) -> Y): LiveData<Y> =
        Transformations.map(this) { transform(it) }

/**
 * Returns a [LiveData] mapped from the input `this` `LiveData` by applying
 * [transform] to each value set on `this`.
 * <p>
 * The returned `LiveData` delegates to the most recent `LiveData` created by
 * [transform] with the most recent value set to `this`, without
 * changing the reference. In this way [transform] can change the 'backing'
 * `LiveData` transparently to any observer registered to the `LiveData` returned
 * by `switchMap()`.
 *
 * Note that when the backing `LiveData` is switched, no further values from the older
 * `LiveData` will be set to the output `LiveData`. In this way, the method is
 * analogous to [io.reactivex.Observable.switchMap].
 *
 * [transform] will be executed on the main thread.
 *
 * Here is an example class that holds a typed-in name of a user
 * `String` (such as from an `EditText`) in a [MutableLiveData] and
 * returns a `LiveData` containing a List of `User` objects for users that have
 * that name. It populates that `LiveData` by requerying a repository-pattern object
 * each time the typed name changes.
 * <p>
 * This `ViewModel` would permit the observing UI to update "live" as the user ID text
 * changes.
 *
 * ```
 * class UserViewModel: AndroidViewModel {
 *     val nameQueryLiveData : MutableLiveData<String> = ...
 *
 *     fun usersWithNameLiveData(): LiveData<List<String>> = nameQueryLiveData.switchMap {
 *         name -> myDataSource.usersWithNameLiveData(name)
 *     }
 *
 *     fun setNameQuery(val name: String) {
 *         this.nameQueryLiveData.value = name;
 *     }
 * }
 * ```
 */
inline fun <X, Y> LiveData<X>.switchMap(
    crossinline transform: (X) -> LiveData<Y>
): LiveData<Y> = Transformations.switchMap(this) { transform(it) }

/**
 * Creates a new [LiveData] object does not emit a value until the source `this` LiveData value
 * has been changed.  The value is considered changed if `equals()` yields `false`.
 */
@Suppress("NOTHING_TO_INLINE")
inline fun <X> LiveData<X>.distinctUntilChanged(): LiveData<X> =
        Transformations.distinctUntilChanged(this)

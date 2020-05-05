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

package androidx.ui.rxjava2

import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.FrameManager
import androidx.compose.State
import androidx.compose.onPreCommit
import androidx.compose.state
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.plugins.RxJavaPlugins

/**
 * Subscribes to this [Observable] and represents its values via [State]. Every time there would
 * be new value posted into the [Observable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Observable.onErrorReturn] or [Observable.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.ObservableSample
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T : Any> Observable<T>.subscribeAsState(): State<T?> = subscribeAsState(null)

/**
 * Subscribes to this [Observable] and represents its values via [State]. Every time there would
 * be new value posted into the [Observable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Observable.onErrorReturn] or [Observable.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.ObservableWithInitialSample
 */
@Composable
fun <R, T : R> Observable<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Flowable] and represents its values via [State]. Every time there would
 * be new value posted into the [Flowable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Flowable.onErrorReturn] or [Flowable.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.FlowableSample
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T : Any> Flowable<T>.subscribeAsState(): State<T?> = subscribeAsState(null)

/**
 * Subscribes to this [Flowable] and represents its values via [State]. Every time there would
 * be new value posted into the [Flowable] the returned [State] will be updated causing
 * recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Flowable.onErrorReturn] or [Flowable.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.FlowableWithInitialSample
 */
@Composable
fun <R, T : R> Flowable<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Single] and represents its value via [State]. Once the value would be
 * posted into the [Single] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Single.onErrorReturn] or [Single.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.SingleSample
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T : Any> Single<T>.subscribeAsState(): State<T?> = subscribeAsState(null)

/**
 * Subscribes to this [Single] and represents its value via [State]. Once the value would be
 * posted into the [Single] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Single.onErrorReturn] or [Single.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.SingleWithInitialSample
 */
@Composable
fun <R, T : R> Single<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Maybe] and represents its value via [State]. Once the value would be
 * posted into the [Maybe] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Maybe.onErrorComplete], [Maybe.onErrorReturn] or [Maybe.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.MaybeSample
 */
@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun <T : Any> Maybe<T>.subscribeAsState(): State<T?> = subscribeAsState(null)

/**
 * Subscribes to this [Maybe] and represents its value via [State]. Once the value would be
 * posted into the [Maybe] the returned [State] will be updated causing recomposition of
 * every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Maybe.onErrorComplete], [Maybe.onErrorReturn] or [Maybe.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.MaybeWithInitialSample
 */
@Composable
fun <R, T : R> Maybe<T>.subscribeAsState(initial: R): State<R> =
    asState(initial) { subscribe(it) }

/**
 * Subscribes to this [Completable] and represents its completed state via [State]. Once the
 * [Completable] will be completed the returned [State] will be updated with `true` value
 * causing recomposition of every [State.value] usage.
 *
 * The internal observer will be automatically disposed when this composable disposes.
 *
 * Note that errors are not handled and the default [RxJavaPlugins.onError] logic will be
 * used. To handle the error in a more meaningful way you can use operators like
 * [Completable.onErrorComplete] or [Completable.onErrorResumeNext].
 *
 * @sample androidx.ui.rxjava2.samples.CompletableSample
 */
@Composable
fun Completable.subscribeAsState(): State<Boolean> =
    asState(false) { callback -> subscribe { callback(true) } }

@Composable
private inline fun <T, S> S.asState(
    initial: T,
    crossinline subscribe: S.((T) -> Unit) -> Disposable
): State<T> {
    val state = state { initial }
    onPreCommit(this) {
        val disposable = subscribe {
            FrameManager.framed { state.value = it }
        }
        onDispose { disposable.dispose() }
    }
    return state
}

// NOTE(lmr): This API is no longer needed in any way by the compiler, but we still need this API
// to be here to support versions of Android Studio that are still looking for it. Without it,
// valid composable code will look broken in the IDE. Remove this after we have left some time to
// get all versions of Studio upgraded.
// b/152059242
@Deprecated(
    "This property should not be called directly. It is only used by the compiler.",
    replaceWith = ReplaceWith("currentComposer")
)
internal val composer: Composer<*>
    get() = error(
        "This property should not be called directly. It is only used by the compiler."
    )

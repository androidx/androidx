/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.paging

import android.annotation.SuppressLint
import androidx.concurrent.futures.ResolvableFuture
import com.google.common.util.concurrent.ListenableFuture
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Action
import io.reactivex.functions.Consumer
import java.util.concurrent.Executor

private class ListenableFutureDisposeListener internal constructor(
    private val future: ListenableFuture<*>,
    private val disposable: Disposable
) : Runnable {
    override fun run() {
        if (future.isCancelled) {
            disposable.dispose()
        }
    }
}

private class ListenableFutureSubscribeConsumer internal constructor(
    private val compositeDisposable: CompositeDisposable
) : Consumer<Disposable> {
    override fun accept(disposable: Disposable) {
        compositeDisposable.add(disposable)
    }
}

private class ListenableFutureDisposeAction<T> internal constructor(
    private val future: ResolvableFuture<T>
) : Action {
    override fun run() {
        future.cancel(true)
    }
}

private class ListenableFutureSuccessConsumer<T> internal constructor(
    private val future: ResolvableFuture<T>
) : Consumer<T> {
    override fun accept(data: T) {
        future.set(data)
    }
}

private class ListenableFutureErrorConsumer<T>
internal constructor(private val future: ResolvableFuture<T>) : Consumer<Throwable> {
    override fun accept(e: Throwable) {
        future.setException(e)
    }
}

@SuppressLint("CheckResult")
internal fun <T> Single<T>.asListenableFuture(
    executor: Executor,
    scheduler: Scheduler
): ListenableFuture<T> {
    val compositeDisposable = CompositeDisposable()
    val future = ResolvableFuture.create<T>()

    future.addListener(ListenableFutureDisposeListener(future, compositeDisposable), executor)

    val successConsumer = ListenableFutureSuccessConsumer(future)
    val errorConsumer = ListenableFutureErrorConsumer(future)
    subscribeOn(scheduler)
        .observeOn(scheduler)
        .doOnSubscribe(ListenableFutureSubscribeConsumer(compositeDisposable))
        .doOnDispose(ListenableFutureDisposeAction(future))
        .subscribe(successConsumer, errorConsumer)

    return future
}

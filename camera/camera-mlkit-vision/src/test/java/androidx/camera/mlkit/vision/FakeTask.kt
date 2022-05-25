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

package androidx.camera.mlkit.vision

import android.app.Activity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import java.util.concurrent.Executor

/**
 * Fake GmsCore [Task] that returns immediately.
 */
class FakeTask<T>(
    private val result: T?,
    private val exception: Exception?
) : Task<T>() {

    override fun isComplete(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isSuccessful(): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCanceled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getResult(): T? {
        return result
    }

    override fun getException(): java.lang.Exception? {
        return exception
    }

    override fun addOnCompleteListener(
        executor: Executor,
        listener: OnCompleteListener<T>
    ): Task<T> {
        executor.execute { listener.onComplete(this) }
        return this
    }

    override fun <X : Throwable?> getResult(exceptionType: Class<X>): T {
        TODO("Not yet implemented")
    }

    override fun addOnSuccessListener(listener: OnSuccessListener<in T>): Task<T> {
        TODO("Not yet implemented")
    }

    override fun addOnSuccessListener(
        executor: Executor,
        listener: OnSuccessListener<in T>
    ): Task<T> {
        TODO("Not yet implemented")
    }

    override fun addOnSuccessListener(
        activity: Activity,
        listener: OnSuccessListener<in T>
    ): Task<T> {
        TODO("Not yet implemented")
    }

    override fun addOnFailureListener(listener: OnFailureListener): Task<T> {
        TODO("Not yet implemented")
    }

    override fun addOnFailureListener(
        executor: Executor,
        listener: OnFailureListener
    ): Task<T> {
        TODO("Not yet implemented")
    }

    override fun addOnFailureListener(
        activity: Activity,
        listener: OnFailureListener
    ): Task<T> {
        TODO("Not yet implemented")
    }
}
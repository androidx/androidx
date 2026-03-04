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

package androidx.annotation.keep.samples

import androidx.annotation.Sampled
import androidx.annotation.keep.UsesReflectionToConstruct

open class PaymentProcessor

class TransactionAccess

@Sampled
fun usesReflectionToConstructSample_default() {
    @UsesReflectionToConstruct(
        className = "com.example.PaymentProcessorImpl",
        parameterTypes = [], // default constructor, no parameters
    )
    fun constructMyType() {
        // impl class isn't always present, instantiate with reflection to attempt to discover
        Class.forName("com.example.PaymentProcessorImpl").getDeclaredConstructor().newInstance()
            as PaymentProcessor
    }
}

@Sampled
fun usesReflectionToConstructSample_parameters() {
    @UsesReflectionToConstruct(
        className = "com.example.PaymentProcessorImpl",
        parameterTypes = [TransactionAccess::class, Long::class],
    )
    fun constructMyType(transactionAccess: TransactionAccess, timeout: Long) {
        // impl class isn't always present, instantiate with reflection to attempt to discover
        Class.forName("com.example.PaymentProcessorImpl")
            .getDeclaredConstructor(TransactionAccess::class.java, Long::class.java)
            .newInstance() as PaymentProcessor
    }
}

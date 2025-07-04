/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.build.gradle.internal.fixtures

import java.util.function.BiFunction
import org.gradle.api.Transformer
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec

class FakeGradleProperty<T>(private var value: T? = null) : Property<T> {

    private var valueProvider: Provider<out T>? = null
    private var convention: T? = null

    override fun <S : Any?> flatMap(
        transformer: Transformer<out Provider<out S>?, in T>
    ): Provider<S> {
        throw NotImplementedError()
    }

    override fun isPresent() = value != null || valueProvider != null

    @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE") // KT-36770
    override fun getOrElse(defaultValue: T) =
        value ?: valueProvider?.get() ?: convention ?: defaultValue

    override fun <S : Any?> map(transformer: Transformer<out S?, in T>): Provider<S> {
        throw NotImplementedError()
    }

    override fun get() =
        value ?: valueProvider?.get() ?: convention ?: throw IllegalStateException("Value not set")

    override fun getOrNull() = value ?: valueProvider?.get() ?: convention

    override fun filter(spec: Spec<in T>): Provider<T> {
        throw NotImplementedError()
    }

    override fun value(value: T?): Property<T> {
        this.value = value
        return this
    }

    override fun set(value: T?) {
        this.value = value
        this.valueProvider = null
    }

    override fun set(provider: Provider<out T>) {
        this.value = null
        this.valueProvider = provider
    }

    override fun convention(convention: T?): Property<T> {
        this.convention = convention
        return this
    }

    override fun convention(convention: Provider<out T>): Property<T> {
        throw NotImplementedError()
    }

    override fun finalizeValue() {
        throw NotImplementedError()
    }

    override fun finalizeValueOnRead() {
        throw NotImplementedError()
    }

    override fun value(p0: Provider<out T>): Property<T> {
        throw NotImplementedError()
    }

    override fun disallowChanges() {
        throw NotImplementedError()
    }

    @Suppress("WRONG_NULLABILITY_FOR_JAVA_OVERRIDE")
    override fun orElse(p0: T): Provider<T> {
        throw NotImplementedError()
    }

    override fun orElse(p0: Provider<out T>): Provider<T> {
        throw NotImplementedError()
    }

    override fun disallowUnsafeRead() {
        throw NotImplementedError()
    }

    override fun unset(): Property<T> {
        throw NotImplementedError()
    }

    override fun unsetConvention(): Property<T> {
        throw NotImplementedError()
    }

    @Deprecated("Deprecated in Java")
    override fun forUseAtConfigurationTime(): Provider<T> {
        throw NotImplementedError()
    }

    override fun <U : Any?, R : Any?> zip(
        p0: Provider<U>,
        p1: BiFunction<in T, in U, out R>,
    ): Provider<R> {
        throw NotImplementedError()
    }
}

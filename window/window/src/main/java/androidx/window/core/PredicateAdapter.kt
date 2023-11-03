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

package androidx.window.core

import android.annotation.SuppressLint
import android.util.Pair
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * An adapter over {@link java.util.function.Predicate} to workaround mismatch in expected extension
 * API signatures after library desugaring. See b/203472665
 */
@SuppressLint("BanUncheckedReflection")
internal class PredicateAdapter(
    private val loader: ClassLoader
) {
    internal fun predicateClassOrNull(): Class<*>? {
        return try {
            predicateClassOrThrow()
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private fun predicateClassOrThrow(): Class<*> {
        return loader.loadClass("java.util.function.Predicate")
    }

    fun <T : Any> buildPredicate(clazz: KClass<T>, predicate: (T) -> Boolean): Any {
        val predicateHandler = PredicateStubHandler(
            clazz,
            predicate
        )
        return Proxy.newProxyInstance(loader, arrayOf(predicateClassOrThrow()), predicateHandler)
    }

    fun <T : Any, U : Any> buildPairPredicate(
        firstClazz: KClass<T>,
        secondClazz: KClass<U>,
        predicate: (T, U) -> Boolean
    ): Any {
        val predicateHandler = PairPredicateStubHandler(
            firstClazz,
            secondClazz,
            predicate
        )

        return Proxy.newProxyInstance(loader, arrayOf(predicateClassOrThrow()), predicateHandler)
    }

    private abstract class BaseHandler<T : Any>(private val clazz: KClass<T>) : InvocationHandler {
        override fun invoke(obj: Any, method: Method, parameters: Array<out Any>?): Any {
            return when {
                method.isTest(parameters) -> {
                    val argument = clazz.cast(parameters?.get(0))
                    invokeTest(obj, argument)
                }
                method.isEquals(parameters) -> {
                    obj === parameters?.get(0)!!
                }
                method.isHashCode(parameters) -> {
                    hashCode()
                }
                method.isToString(parameters) -> {
                    toString()
                }
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$obj, method: $method, args: $parameters"
                    )
                }
            }
        }

        abstract fun invokeTest(obj: Any, parameter: T): Boolean

        protected fun Method.isEquals(args: Array<out Any>?): Boolean {
            return name == "equals" && returnType.equals(Boolean::class.java) && args?.size == 1
        }

        protected fun Method.isHashCode(args: Array<out Any>?): Boolean {
            return name == "hashCode" && returnType.equals(Int::class.java) && args == null
        }

        protected fun Method.isTest(args: Array<out Any>?): Boolean {
            return name == "test" && returnType.equals(Boolean::class.java) && args?.size == 1
        }

        protected fun Method.isToString(args: Array<out Any>?): Boolean {
            return name == "toString" && returnType.equals(String::class.java) && args == null
        }
    }

    private class PredicateStubHandler<T : Any>(
        clazzT: KClass<T>,
        private val predicate: (T) -> Boolean
    ) : BaseHandler<T>(clazzT) {
        override fun invokeTest(obj: Any, parameter: T): Boolean {
            return predicate(parameter)
        }

        override fun hashCode(): Int {
            return predicate.hashCode()
        }

        override fun toString(): String {
            return predicate.toString()
        }
    }

    private class PairPredicateStubHandler<T : Any, U : Any>(
        private val clazzT: KClass<T>,
        private val clazzU: KClass<U>,
        private val predicate: (T, U) -> Boolean
    ) : BaseHandler<Pair<*, *>>(Pair::class) {
        override fun invokeTest(obj: Any, parameter: Pair<*, *>): Boolean {
            val t = clazzT.cast(parameter.first)
            val u = clazzU.cast(parameter.second)
            return predicate(t, u)
        }

        override fun hashCode(): Int {
            return predicate.hashCode()
        }

        override fun toString(): String {
            return predicate.toString()
        }
    }
}

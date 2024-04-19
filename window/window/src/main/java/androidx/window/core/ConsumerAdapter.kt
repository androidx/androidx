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
import android.app.Activity
import android.content.Context
import androidx.annotation.CheckResult
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.cast

/**
 * An adapter over {@link java.util.function.Consumer} to workaround mismatch in expected extension
 * API signatures after library desugaring. See b/203472665
 */
@SuppressLint("BanUncheckedReflection")
internal class ConsumerAdapter(
    private val loader: ClassLoader
) {
    internal fun consumerClassOrNull(): Class<*>? {
        return try {
            unsafeConsumerClass()
        } catch (e: ClassNotFoundException) {
            null
        }
    }

    private fun unsafeConsumerClass(): Class<*> {
        return loader.loadClass("java.util.function.Consumer")
    }

    internal interface Subscription {
        fun dispose()
    }

    private fun <T : Any> buildConsumer(clazz: KClass<T>, consumer: (T) -> Unit): Any {
        val handler = ConsumerHandler(clazz, consumer)
        return Proxy.newProxyInstance(loader, arrayOf(unsafeConsumerClass()), handler)
    }

    fun <T : Any> addConsumer(
        obj: Any,
        clazz: KClass<T>,
        methodName: String,
        consumer: (T) -> Unit
    ) {
        obj.javaClass.getMethod(methodName, unsafeConsumerClass())
            .invoke(obj, buildConsumer(clazz, consumer))
    }

    @CheckResult
    fun <T : Any> createSubscription(
        obj: Any,
        clazz: KClass<T>,
        addMethodName: String,
        removeMethodName: String,
        activity: Activity,
        consumer: (T) -> Unit
    ): Subscription {
        val javaConsumer = buildConsumer(clazz, consumer)
        obj.javaClass.getMethod(addMethodName, Activity::class.java, unsafeConsumerClass())
            .invoke(obj, activity, javaConsumer)
        val removeMethod = obj.javaClass.getMethod(removeMethodName, unsafeConsumerClass())
        return object : Subscription {
            override fun dispose() {
                removeMethod.invoke(obj, javaConsumer)
            }
        }
    }

    @CheckResult
    fun <T : Any> createSubscriptionNoActivity(
        obj: Any,
        clazz: KClass<T>,
        addMethodName: String,
        removeMethodName: String,
        consumer: (T) -> Unit
    ): Subscription {
        val javaConsumer = buildConsumer(clazz, consumer)
        obj.javaClass.getMethod(addMethodName, unsafeConsumerClass())
            .invoke(obj, javaConsumer)
        val removeMethod = obj.javaClass.getMethod(removeMethodName, unsafeConsumerClass())
        return object : Subscription {
            override fun dispose() {
                removeMethod.invoke(obj, javaConsumer)
            }
        }
    }

    @CheckResult
    fun <T : Any> createSubscription(
        obj: Any,
        clazz: KClass<T>,
        addMethodName: String,
        removeMethodName: String,
        context: Context,
        consumer: (T) -> Unit
    ): Subscription {
        val javaConsumer = buildConsumer(clazz, consumer)
        obj.javaClass.getMethod(addMethodName, Context::class.java, unsafeConsumerClass())
            .invoke(obj, context, javaConsumer)
        val removeMethod = obj.javaClass.getMethod(removeMethodName, unsafeConsumerClass())
        return object : Subscription {
            override fun dispose() {
                removeMethod.invoke(obj, javaConsumer)
            }
        }
    }

    /**
     * Similar to {@link #createSubscription} but without needing to provide
     * a {@code removeMethodName} due to it being handled on the extensions side
     */
    fun <T : Any> createConsumer(
        obj: Any,
        clazz: KClass<T>,
        addMethodName: String,
        activity: Activity,
        consumer: (T) -> Unit
    ) {
        val javaConsumer = buildConsumer(clazz, consumer)
        obj.javaClass.getMethod(addMethodName, Activity::class.java, unsafeConsumerClass())
            .invoke(obj, activity, javaConsumer)
        }

    private class ConsumerHandler<T : Any>(
        private val clazz: KClass<T>,
        private val consumer: (T) -> Unit
    ) : InvocationHandler {
        override fun invoke(obj: Any, method: Method, parameters: Array<out Any>?): Any {
            return when {
                method.isAccept(parameters) -> {
                    val argument = clazz.cast(parameters?.get(0))
                    invokeAccept(argument)
                }
                method.isEquals(parameters) -> {
                    obj === parameters?.get(0)
                }
                method.isHashCode(parameters) -> {
                    consumer.hashCode()
                }
                method.isToString(parameters) -> {
                    // MulticastConsumer#accept must not be obfuscated by proguard if kotlin-reflect
                    // is included. Otherwise, invocation of consumer#toString (e.g. by the library
                    // or by the on-device implementation) will crash due to kotlin-reflect not
                    // finding MulticastConsumer#accept.
                    consumer.toString()
                }
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$obj, method: $method, args: $parameters"
                    )
                }
            }
        }

        fun invokeAccept(parameter: T) {
            consumer(parameter)
        }

        private fun Method.isEquals(args: Array<out Any>?): Boolean {
            return name == "equals" && returnType.equals(Boolean::class.java) && args?.size == 1
        }

        private fun Method.isHashCode(args: Array<out Any>?): Boolean {
            return name == "hashCode" && returnType.equals(Int::class.java) && args == null
        }

        private fun Method.isAccept(args: Array<out Any>?): Boolean {
            return name == "accept" && args?.size == 1
        }

        private fun Method.isToString(args: Array<out Any>?): Boolean {
            return name == "toString" && returnType.equals(String::class.java) && args == null
        }
    }
}

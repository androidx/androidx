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

package androidx.navigation

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.collection.ArrayMap
import java.lang.reflect.Method
import kotlin.reflect.KClass

internal val methodSignature = arrayOf(Bundle::class.java)
internal val methodMap = ArrayMap<KClass<out NavArgs>, Method>()

/**
 * An implementation of [Lazy] used by [android.app.Activity.navArgs] and
 * [androidx.fragment.app.Fragment.navArgs].
 *
 * [argumentProducer] is a lambda that will be called during initialization to provide
 * arguments to construct an [Args] instance via reflection.
 */
public class NavArgsLazy<Args : NavArgs>(
    private val navArgsClass: KClass<Args>,
    private val argumentProducer: () -> Bundle
) : Lazy<Args> {
    private var cached: Args? = null

    override val value: Args
        get() {
            var args = cached
            if (args == null) {
                val arguments = argumentProducer()
                val method: Method = methodMap[navArgsClass]
                    ?: navArgsClass.java.getMethod("fromBundle", *methodSignature).also { method ->
                        // Save a reference to the method
                        methodMap[navArgsClass] = method
                    }

                @SuppressLint("BanUncheckedReflection") // needed for method.invoke
                @Suppress("UNCHECKED_CAST")
                args = method.invoke(null, arguments) as Args
                cached = args
            }
            return args
        }

    override fun isInitialized(): Boolean = cached != null
}

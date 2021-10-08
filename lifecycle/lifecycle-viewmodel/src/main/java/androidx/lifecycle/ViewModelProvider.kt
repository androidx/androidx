/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.DEFAULT_KEY
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.defaultFactory
import androidx.lifecycle.viewmodel.CreationExtras.Key
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.viewmodel.CombinedCreationExtras
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.lang.UnsupportedOperationException
import java.lang.reflect.InvocationTargetException

/**
 * An utility class that provides `ViewModels` for a scope.
 *
 * Default `ViewModelProvider` for an `Activity` or a `Fragment` can be obtained
 * by passing it to the constructor: `ViewModelProvider(myFragment)`
 */
public open class ViewModelProvider
/**
 * Creates a ViewModelProvider
 *
 * @param store `ViewModelStore` where ViewModels will be stored.
 * @param factory factory a `Factory` which will be used to instantiate new `ViewModels`
 * @param defaultCreationExtras extras to pass to a factory
 */
@JvmOverloads
constructor(
    private val store: ViewModelStore,
    private val factory: Factory,
    private val defaultCreationExtras: CreationExtras = CreationExtras.Empty,
) {
    /**
     * Implementations of `Factory` interface are responsible to instantiate ViewModels.
     */
    public interface Factory {
        /**
         * Creates a new instance of the given `Class`.
         *
         * Default implementation throws [UnsupportedOperationException].
         *
         * @param modelClass a `Class` whose instance is requested
         * @return a newly created ViewModel
         */
        public fun <T : ViewModel> create(modelClass: Class<T>): T {
            throw UnsupportedOperationException(
                "Factory.create(String) is unsupported.  This Factory requires " +
                    "`CreationExtras` to be passed into `create` method."
            )
        }

        /**
         * Creates a new instance of the given `Class`.
         *
         * @param modelClass a `Class` whose instance is requested
         * @param extras an additional information for this creation request
         * @return a newly created ViewModel
         */
        public fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            create(modelClass)
    }

    /**
     * @suppress
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public open class OnRequeryFactory {
        public open fun onRequery(viewModel: ViewModel) {}
    }

    /**
     * Creates `ViewModelProvider`. This will create `ViewModels`
     * and retain them in a store of the given `ViewModelStoreOwner`.
     *
     *
     * This method will use the
     * [default factory][HasDefaultViewModelProviderFactory.getDefaultViewModelProviderFactory]
     * if the owner implements [HasDefaultViewModelProviderFactory]. Otherwise, a
     * [NewInstanceFactory] will be used.
     */
    public constructor(
        owner: ViewModelStoreOwner
    ) : this(owner.viewModelStore, defaultFactory(owner), defaultCreationExtras(owner))

    /**
     * Creates `ViewModelProvider`, which will create `ViewModels` via the given
     * `Factory` and retain them in a store of the given `ViewModelStoreOwner`.
     *
     * @param owner   a `ViewModelStoreOwner` whose [ViewModelStore] will be used to
     * retain `ViewModels`
     * @param factory a `Factory` which will be used to instantiate
     * new `ViewModels`
     */
    public constructor(owner: ViewModelStoreOwner, factory: Factory) : this(
        owner.viewModelStore,
        factory,
        defaultCreationExtras(owner)
    )

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     * @throws IllegalArgumentException if the given [modelClass] is local or anonymous class.
     */
    @MainThread
    public open operator fun <T : ViewModel> get(modelClass: Class<T>): T {
        val canonicalName = modelClass.canonicalName
            ?: throw IllegalArgumentException("Local and anonymous classes can not be ViewModels")
        return get("$DEFAULT_KEY:$canonicalName", modelClass)
    }

    /**
     * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
     * an activity), associated with this `ViewModelProvider`.
     *
     * The created ViewModel is associated with the given scope and will be retained
     * as long as the scope is alive (e.g. if it is an activity, until it is
     * finished or process is killed).
     *
     * @param key        The key to use to identify the ViewModel.
     * @param modelClass The class of the ViewModel to create an instance of it if it is not
     * present.
     * @return A ViewModel that is an instance of the given type `T`.
     */
    @Suppress("UNCHECKED_CAST")
    @MainThread
    public open operator fun <T : ViewModel> get(key: String, modelClass: Class<T>): T {
        val viewModel = store[key]
        if (modelClass.isInstance(viewModel)) {
            (factory as? OnRequeryFactory)?.onRequery(viewModel)
            return viewModel as T
        } else {
            @Suppress("ControlFlowWithEmptyBody")
            if (viewModel != null) {
                // TODO: log a warning.
            }
        }
        val extras = MutableCreationExtras()
        extras[VIEW_MODEL_KEY] = key
        return factory.create(
            modelClass,
            CombinedCreationExtras(extras, defaultCreationExtras)
        ).also { store.put(key, it) }
    }

    /**
     * Simple factory, which calls empty constructor on the give class.
     */
    // actually there is getInstance()
    @Suppress("SingletonConstructor")
    public open class NewInstanceFactory : Factory {
        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return try {
                modelClass.newInstance()
            } catch (e: InstantiationException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            }
        }

        public companion object {
            private var sInstance: NewInstanceFactory? = null

            /**
             * @suppress
             * Retrieve a singleton instance of NewInstanceFactory.
             *
             * @return A valid [NewInstanceFactory]
             */
            @JvmStatic
            public val instance: NewInstanceFactory
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                get() {
                    if (sInstance == null) {
                        sInstance = NewInstanceFactory()
                    }
                    return sInstance!!
                }

            private object ViewModelKeyImpl : Key<String>
            /**
             * A [CreationExtras.Key] to get a key associated with a requested
             * `ViewModel` from [CreationExtras]
             *
             *  `ViewModelProvider` automatically puts a key that was passed to
             *  `ViewModelProvider.get(key, MyViewModel::class.java)`
             *  or generated in `ViewModelProvider.get(MyViewModel::class.java)` to the `CreationExtras` that
             *  are passed to [ViewModelProvider.Factory].
             */
            @JvmField
            val VIEW_MODEL_KEY: Key<String> = ViewModelKeyImpl
        }
    }

    /**
     * [Factory] which may create [AndroidViewModel] and
     * [ViewModel], which have an empty constructor.
     *
     * @param application an application to pass in [AndroidViewModel]
     */
    public open class AndroidViewModelFactory(
        private val application: Application
    ) : NewInstanceFactory() {
        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (AndroidViewModel::class.java.isAssignableFrom(modelClass)) {
                try {
                    modelClass.getConstructor(Application::class.java).newInstance(application)
                } catch (e: NoSuchMethodException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: IllegalAccessException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InstantiationException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                } catch (e: InvocationTargetException) {
                    throw RuntimeException("Cannot create an instance of $modelClass", e)
                }
            } else super.create(modelClass)
        }

        public companion object {
            internal fun defaultFactory(owner: ViewModelStoreOwner): Factory =
                if (owner is HasDefaultViewModelProviderFactory)
                    owner.defaultViewModelProviderFactory else instance

            internal const val DEFAULT_KEY = "androidx.lifecycle.ViewModelProvider.DefaultKey"

            private var sInstance: AndroidViewModelFactory? = null

            /**
             * Retrieve a singleton instance of AndroidViewModelFactory.
             *
             * @param application an application to pass in [AndroidViewModel]
             * @return A valid [AndroidViewModelFactory]
             */
            @JvmStatic
            public fun getInstance(application: Application): AndroidViewModelFactory {
                if (sInstance == null) {
                    sInstance = AndroidViewModelFactory(application)
                }
                return sInstance!!
            }
        }
    }
}

internal fun defaultCreationExtras(owner: ViewModelStoreOwner): CreationExtras {
    return if (owner is HasDefaultViewModelProviderFactory) {
        owner.defaultViewModelCreationExtras
    } else CreationExtras.Empty
}

/**
 * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
 * an activity), associated with this `ViewModelProvider`.
 *
 * @see ViewModelProvider.get(Class)
 */
@MainThread
public inline fun <reified VM : ViewModel> ViewModelProvider.get(): VM = get(VM::class.java)
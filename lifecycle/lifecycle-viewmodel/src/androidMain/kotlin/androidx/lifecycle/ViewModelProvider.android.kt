/*
 * Copyright 2024 The Android Open Source Project
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
@file:JvmName("ViewModelProvider")

package androidx.lifecycle

import android.app.Application
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.CreationExtras.Key
import androidx.lifecycle.viewmodel.InitializerViewModelFactory
import androidx.lifecycle.viewmodel.ViewModelInitializer
import androidx.lifecycle.viewmodel.ViewModelProviderImpl
import androidx.lifecycle.viewmodel.internal.JvmViewModelProviders
import androidx.lifecycle.viewmodel.internal.ViewModelProviders
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KClass

public actual open class ViewModelProvider private constructor(
    private val impl: ViewModelProviderImpl,
) {

    /**
     * Creates a [ViewModelProvider]. This provider generates [ViewModel] instances using the
     * specified [Factory] and stores them within the [ViewModelStore] of the provided
     * [ViewModelStoreOwner].
     *
     * @param store `ViewModelStore` where ViewModels will be stored.
     * @param factory The [Factory] responsible for creating new [ViewModel] instances.
     * @param defaultCreationExtras Additional data to be passed to the [Factory] during
     *  [ViewModel] creation.
     */
    @JvmOverloads
    public constructor(
        store: ViewModelStore,
        factory: Factory,
        defaultCreationExtras: CreationExtras = CreationExtras.Empty,
    ) : this(ViewModelProviderImpl(store, factory, defaultCreationExtras))

    /**
     * Creates [ViewModelProvider]. This will create [ViewModel] instances and retain them in the
     * [ViewModelStore] of the given [ViewModelStoreOwner].
     *
     * This method will use the
     * [default factory][HasDefaultViewModelProviderFactory.defaultViewModelProviderFactory]
     * if the owner implements [HasDefaultViewModelProviderFactory]. Otherwise, a
     * [NewInstanceFactory] will be used.
     */
    public constructor(
        owner: ViewModelStoreOwner,
    ) : this(
        store = owner.viewModelStore,
        factory = ViewModelProviders.getDefaultFactory(owner),
        defaultCreationExtras = ViewModelProviders.getDefaultCreationExtras(owner)
    )

    /**
     * Creates a [ViewModelProvider]. This provider generates [ViewModel] instances using the
     * specified [Factory] and stores them within the [ViewModelStore] of the provided
     * [ViewModelStoreOwner].
     *
     * @param owner The [ViewModelStoreOwner] that will manage the lifecycle of the created
     *  [ViewModel] instances.
     * @param factory The [Factory] responsible for creating new [ViewModel] instances.
     */
    public constructor(
        owner: ViewModelStoreOwner,
        factory: Factory,
    ) : this(
        store = owner.viewModelStore,
        factory = factory,
        defaultCreationExtras = ViewModelProviders.getDefaultCreationExtras(owner)
    )

    @MainThread
    public actual operator fun <T : ViewModel> get(modelClass: KClass<T>): T =
        impl.getViewModel(modelClass)

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
    public open operator fun <T : ViewModel> get(modelClass: Class<T>): T =
        get(modelClass.kotlin)

    @MainThread
    public actual operator fun <T : ViewModel> get(key: String, modelClass: KClass<T>): T =
        impl.getViewModel(modelClass, key)

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
    @MainThread
    public open operator fun <T : ViewModel> get(key: String, modelClass: Class<T>): T =
        impl.getViewModel(modelClass.kotlin, key)

    public actual interface Factory {

        /**
         * Creates a new instance of the given `Class`.
         *
         * Default implementation throws [UnsupportedOperationException].
         *         ˆ
         * @param modelClass a `Class` whose instance is requested
         * @return a newly created ViewModel
         */
        public fun <T : ViewModel> create(modelClass: Class<T>): T =
            ViewModelProviders.unsupportedCreateViewModel()

        /**
         * Creates a new instance of the given `Class`.
         *
         * @param modelClass a `Class` whose instance is requested
         * @param extras an additional information for this creation request
         * @return a newly created ViewModel
         */
        public fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            create(modelClass)

        public actual fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T = create(modelClass.java, extras)

        public companion object {
            /**
             * Creates an [InitializerViewModelFactory] using the given initializers.
             *
             * @param initializers the class initializer pairs used for the factory to create
             * simple view models
             *
             * @see [InitializerViewModelFactory]
             */
            @JvmStatic
            public fun from(vararg initializers: ViewModelInitializer<*>): Factory =
                ViewModelProviders.createInitializerFactory(*initializers)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public actual open class OnRequeryFactory {
        public actual open fun onRequery(viewModel: ViewModel) {}
    }

    /**
     * Simple factory, which calls empty constructor on the give class.
     */
    public open class NewInstanceFactory
    /**
     * Construct a new [NewInstanceFactory] instance.
     *
     * Use [NewInstanceFactory.instance] to get a default instance of [NewInstanceFactory].
     */
    @Suppress("SingletonConstructor")
    constructor() : Factory {

        public override fun <T : ViewModel> create(modelClass: Class<T>): T =
            JvmViewModelProviders.createViewModel(modelClass)

        public override fun <T : ViewModel> create(
            modelClass: Class<T>,
            extras: CreationExtras,
        ): T = create(modelClass)

        public override fun <T : ViewModel> create(
            modelClass: KClass<T>,
            extras: CreationExtras,
        ): T = create(modelClass.java, extras)

        public companion object {
            private var _instance: NewInstanceFactory? = null

            /**
             * Retrieve a singleton instance of NewInstanceFactory.
             *
             * @return A valid [NewInstanceFactory]
             */
            @JvmStatic
            public val instance: NewInstanceFactory
                @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
                get() {
                    if (_instance == null) {
                        _instance = NewInstanceFactory()
                    }
                    return _instance!!
                }

            /**
             * A [CreationExtras.Key] used to retrieve the key associated with a requested [ViewModel].
             *
             * The [ViewModelProvider] automatically includes the key in the [CreationExtras] passed to
             * [ViewModelProvider.Factory]. This applies to keys generated by either of these usage
             * patterns:
             * - `ViewModelProvider.get(key, MyViewModel::class)`: provided `key` is used.
             * - `ViewModelProvider.get(MyViewModel::class)`: generates a `key` from given `class`.
             *
             * @see ViewModelProvider.VIEW_MODEL_KEY
             */
            @JvmField
            public val VIEW_MODEL_KEY: Key<String> = ViewModelProviders.ViewModelKey
        }
    }

    /**
     * [Factory] which may create [AndroidViewModel] and
     * [ViewModel], which have an empty constructor.
     *
     * @param application an application to pass in [AndroidViewModel]
     */
    public open class AndroidViewModelFactory
    private constructor(
        private val application: Application?,
        // parameter to avoid clash between constructors with nullable and non-nullable
        // Application
        @Suppress("UNUSED_PARAMETER") unused: Int,
    ) : NewInstanceFactory() {

        /**
         * Constructs this factory.
         * When a factory is constructed this way, a component for which [ViewModel] is created
         * must provide an [Application] by [APPLICATION_KEY] in [CreationExtras], otherwise
         *  [IllegalArgumentException] will be thrown from [create] method.
         */
        @Suppress("SingletonConstructor")
        public constructor() : this(application = null, unused = 0)

        /**
         * Constructs this factory.
         *
         * @param application an application to pass in [AndroidViewModel]
         */
        @Suppress("SingletonConstructor")
        public constructor(application: Application) : this(application, unused = 0)

        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
            return if (application != null) {
                create(modelClass)
            } else {
                val application = extras[APPLICATION_KEY]
                if (application != null) {
                    create(modelClass, application)
                } else {
                    // For AndroidViewModels, CreationExtras must have an application set
                    if (AndroidViewModel::class.java.isAssignableFrom(modelClass)) {
                        throw IllegalArgumentException(
                            "CreationExtras must have an application by `APPLICATION_KEY`"
                        )
                    }
                    super.create(modelClass)
                }
            }
        }

        @Suppress("DocumentExceptions")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return if (application == null) {
                throw UnsupportedOperationException(
                    "AndroidViewModelFactory constructed " +
                        "with empty constructor works only with " +
                        "create(modelClass: Class<T>, extras: CreationExtras)."
                )
            } else {
                create(modelClass, application)
            }
        }

        @Suppress("DocumentExceptions")
        private fun <T : ViewModel> create(modelClass: Class<T>, app: Application): T {
            return if (AndroidViewModel::class.java.isAssignableFrom(modelClass)) {
                try {
                    modelClass.getConstructor(Application::class.java).newInstance(app)
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
            private var _instance: AndroidViewModelFactory? = null

            /**
             * Retrieve a singleton instance of AndroidViewModelFactory.
             *
             * @param application an application to pass in [AndroidViewModel]
             * @return A valid [AndroidViewModelFactory]
             */
            @JvmStatic
            public fun getInstance(application: Application): AndroidViewModelFactory {
                if (_instance == null) {
                    _instance = AndroidViewModelFactory(application)
                }
                return _instance!!
            }

            /**
             * A [CreationExtras.Key] to query an application in which ViewModel is being created.
             */
            @JvmField
            public val APPLICATION_KEY: Key<Application> = object : Key<Application> {}
        }
    }

    public actual companion object {
        @JvmStatic
        public actual fun create(
            owner: ViewModelStoreOwner,
            factory: Factory,
            extras: CreationExtras,
        ): ViewModelProvider = ViewModelProvider(owner.viewModelStore, factory, extras)

        @JvmStatic
        public actual fun create(
            store: ViewModelStore,
            factory: Factory,
            extras: CreationExtras
        ): ViewModelProvider = ViewModelProvider(store, factory, extras)

        @JvmField
        public actual val VIEW_MODEL_KEY: Key<String> = ViewModelProviders.ViewModelKey
    }
}

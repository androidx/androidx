/*
 * Copyright 2018 The Android Open Source Project
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
@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package androidx.lifecycle

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import androidx.annotation.RestrictTo
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.getInstance
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory.Companion.instance
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException

/**
 * [androidx.lifecycle.ViewModelProvider.Factory] that can create ViewModels accessing and
 * contributing to a saved state via [SavedStateHandle] received in a constructor. If `defaultArgs`
 * bundle was passed into the constructor, it will provide default values in `SavedStateHandle`.
 *
 * If ViewModel is instance of [androidx.lifecycle.AndroidViewModel], it looks for a constructor
 * that receives an [Application] and [SavedStateHandle] (in this order), otherwise it looks for a
 * constructor that receives [SavedStateHandle] only. [androidx.lifecycle.AndroidViewModel] is only
 * supported if you pass a non-null [Application] instance.
 */
class SavedStateViewModelFactory : ViewModelProvider.OnRequeryFactory, ViewModelProvider.Factory {
    private var application: Application? = null
    private val factory: ViewModelProvider.Factory
    private var defaultArgs: Bundle? = null
    private var lifecycle: Lifecycle? = null
    private var savedStateRegistry: SavedStateRegistry? = null

    /**
     * Constructs this factory.
     *
     * When a factory is constructed this way, a component for which [SavedStateHandle] is scoped
     * must have called [enableSavedStateHandles].
     *
     * @see [createSavedStateHandle] docs for more details.
     */
    constructor() {
        factory = ViewModelProvider.AndroidViewModelFactory()
    }

    /**
     * Creates [SavedStateViewModelFactory].
     *
     * [androidx.lifecycle.ViewModel] created with this factory can access to saved state scoped to
     * the given `activity`.
     *
     * @param application an application. If null, [AndroidViewModel] instances will not be
     *   supported.
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     *   [ViewModels][androidx.lifecycle.ViewModel]
     */
    constructor(
        application: Application?,
        owner: SavedStateRegistryOwner
    ) : this(application, owner, null)

    /**
     * Creates [SavedStateViewModelFactory].
     *
     * [androidx.lifecycle.ViewModel] created with this factory can access to saved state scoped to
     * the given `activity`.
     *
     * When a factory is constructed this way, if you add any [CreationExtras] those arguments will
     * be used instead of the state passed in here. It is not possible to mix the arguments received
     * here with the [CreationExtras].
     *
     * @param application an application. If null, [AndroidViewModel] instances will not be
     *   supported.
     * @param owner [SavedStateRegistryOwner] that will provide restored state for created
     *   [ViewModels][androidx.lifecycle.ViewModel]
     * @param defaultArgs values from this `Bundle` will be used as defaults by [SavedStateHandle]
     *   if there is no previously saved state or previously saved state misses a value by such key.
     */
    @SuppressLint("LambdaLast")
    constructor(application: Application?, owner: SavedStateRegistryOwner, defaultArgs: Bundle?) {
        savedStateRegistry = owner.savedStateRegistry
        lifecycle = owner.lifecycle
        this.defaultArgs = defaultArgs
        this.application = application
        factory =
            if (application != null) getInstance(application)
            else ViewModelProvider.AndroidViewModelFactory()
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if the provided extras do not provide a
     *   [ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
     */
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val key =
            extras[ViewModelProvider.NewInstanceFactory.VIEW_MODEL_KEY]
                ?: throw IllegalStateException(
                    "VIEW_MODEL_KEY must always be provided by ViewModelProvider"
                )

        return if (
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] != null &&
                extras[VIEW_MODEL_STORE_OWNER_KEY] != null
        ) {
            val application = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            val isAndroidViewModel = AndroidViewModel::class.java.isAssignableFrom(modelClass)
            val constructor: Constructor<T>? =
                if (isAndroidViewModel && application != null) {
                    findMatchingConstructor(modelClass, ANDROID_VIEWMODEL_SIGNATURE)
                } else {
                    findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE)
                }
            // doesn't need SavedStateHandle
            if (constructor == null) {
                return factory.create(modelClass, extras)
            }
            val viewModel =
                if (isAndroidViewModel && application != null) {
                    newInstance(
                        modelClass,
                        constructor,
                        application,
                        extras.createSavedStateHandle()
                    )
                } else {
                    newInstance(modelClass, constructor, extras.createSavedStateHandle())
                }
            viewModel
        } else {
            val viewModel =
                if (lifecycle != null) {
                    create(key, modelClass)
                } else {
                    throw IllegalStateException(
                        "SAVED_STATE_REGISTRY_OWNER_KEY and" +
                            "VIEW_MODEL_STORE_OWNER_KEY must be provided in the creation extras to" +
                            "successfully create a ViewModel."
                    )
                }
            viewModel
        }
    }

    /**
     * Creates a new instance of the given `Class`.
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a `Class` whose instance is requested
     * @return a newly created ViewModel
     * @throws UnsupportedOperationException if the there is no lifecycle
     */
    fun <T : ViewModel> create(key: String, modelClass: Class<T>): T {
        // empty constructor was called.
        val lifecycle =
            lifecycle
                ?: throw UnsupportedOperationException(
                    "SavedStateViewModelFactory constructed with empty constructor supports only " +
                        "calls to create(modelClass: Class<T>, extras: CreationExtras)."
                )
        val isAndroidViewModel = AndroidViewModel::class.java.isAssignableFrom(modelClass)
        val constructor: Constructor<T>? =
            if (isAndroidViewModel && application != null) {
                findMatchingConstructor(modelClass, ANDROID_VIEWMODEL_SIGNATURE)
            } else {
                findMatchingConstructor(modelClass, VIEWMODEL_SIGNATURE)
            }
        // doesn't need SavedStateHandle
        constructor
            ?: // If you are using a stateful constructor and no application is available, we
            // use an instance factory instead.
            return if (application != null) factory.create(modelClass)
            else instance.create(modelClass)
        val controller =
            LegacySavedStateHandleController.create(
                savedStateRegistry!!,
                lifecycle,
                key,
                defaultArgs
            )
        val viewModel: T =
            if (isAndroidViewModel && application != null) {
                newInstance(modelClass, constructor, application!!, controller.handle)
            } else {
                newInstance(modelClass, constructor, controller.handle)
            }
        viewModel.addCloseable(
            LegacySavedStateHandleController.TAG_SAVED_STATE_HANDLE_CONTROLLER,
            controller
        )
        return viewModel
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException if the given modelClass does not have a classname
     */
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ViewModelProvider calls correct create that support same modelClass with different keys
        // If a developer manually calls this method, there is no "key" in picture, so factory
        // simply uses classname internally as as key.
        val canonicalName =
            modelClass.canonicalName
                ?: throw IllegalArgumentException(
                    "Local and anonymous classes can not be ViewModels"
                )
        return create(canonicalName, modelClass)
    }

    /**  */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    override fun onRequery(viewModel: ViewModel) {
        // needed only for legacy path
        if (lifecycle != null) {
            LegacySavedStateHandleController.attachHandleIfNeeded(
                viewModel,
                savedStateRegistry!!,
                lifecycle!!
            )
        }
    }
}

internal fun <T : ViewModel?> newInstance(
    modelClass: Class<T>,
    constructor: Constructor<T>,
    vararg params: Any
): T {
    return try {
        constructor.newInstance(*params)
    } catch (e: IllegalAccessException) {
        throw RuntimeException("Failed to access $modelClass", e)
    } catch (e: InstantiationException) {
        throw RuntimeException("A $modelClass cannot be instantiated.", e)
    } catch (e: InvocationTargetException) {
        throw RuntimeException("An exception happened in constructor of $modelClass", e.cause)
    }
}

private val ANDROID_VIEWMODEL_SIGNATURE =
    listOf<Class<*>>(Application::class.java, SavedStateHandle::class.java)
private val VIEWMODEL_SIGNATURE = listOf<Class<*>>(SavedStateHandle::class.java)

// it is done instead of getConstructor(), because getConstructor() throws an exception
// if there is no such constructor, which is expensive
internal fun <T> findMatchingConstructor(
    modelClass: Class<T>,
    signature: List<Class<*>>
): Constructor<T>? {
    for (constructor in modelClass.constructors) {
        val parameterTypes = constructor.parameterTypes.toList()
        if (signature == parameterTypes) {
            @Suppress("UNCHECKED_CAST") return constructor as Constructor<T>
        }
        if (signature.size == parameterTypes.size && parameterTypes.containsAll(signature)) {
            throw UnsupportedOperationException(
                "Class ${modelClass.simpleName} must have parameters in the proper " +
                    "order: $signature"
            )
        }
    }
    return null
}

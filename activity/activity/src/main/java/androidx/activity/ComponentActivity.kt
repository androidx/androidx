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
package androidx.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.IntentSender.SendIntentException
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnDrawListener
import android.view.Window
import androidx.activity.contextaware.ContextAware
import androidx.activity.contextaware.ContextAwareHelper
import androidx.activity.contextaware.OnContextAvailableListener
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.ACTION_REQUEST_PERMISSIONS
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.EXTRA_PERMISSIONS
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions.Companion.EXTRA_PERMISSION_GRANT_RESULTS
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult.Companion.EXTRA_ACTIVITY_OPTIONS_BUNDLE
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.ACTION_INTENT_SENDER_REQUEST
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.EXTRA_INTENT_SENDER_REQUEST
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult.Companion.EXTRA_SEND_INTENT_EXCEPTION
import androidx.annotation.CallSuper
import androidx.annotation.ContentView
import androidx.annotation.LayoutRes
import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.app.OnMultiWindowModeChangedProvider
import androidx.core.app.OnNewIntentProvider
import androidx.core.app.OnPictureInPictureModeChangedProvider
import androidx.core.app.OnUserLeaveHintProvider
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.content.OnConfigurationChangedProvider
import androidx.core.content.OnTrimMemoryProvider
import androidx.core.util.Consumer
import androidx.core.view.MenuHost
import androidx.core.view.MenuHostHelper
import androidx.core.view.MenuProvider
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ReportFragment
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.setViewTreeNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.tracing.Trace
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base class for activities that enables composition of higher level components.
 *
 * Rather than all functionality being built directly into this class, only the minimal set of lower
 * level building blocks are included. Higher level components can then be used as needed without
 * enforcing a deep Activity class hierarchy or strong coupling between components.
 */
open class ComponentActivity() :
    androidx.core.app.ComponentActivity(),
    ContextAware,
    LifecycleOwner,
    ViewModelStoreOwner,
    HasDefaultViewModelProviderFactory,
    SavedStateRegistryOwner,
    OnBackPressedDispatcherOwner,
    NavigationEventDispatcherOwner,
    ActivityResultRegistryOwner,
    ActivityResultCaller,
    OnConfigurationChangedProvider,
    OnTrimMemoryProvider,
    OnNewIntentProvider,
    OnMultiWindowModeChangedProvider,
    OnPictureInPictureModeChangedProvider,
    OnUserLeaveHintProvider,
    MenuHost,
    FullyDrawnReporterOwner {
    internal class NonConfigurationInstances {
        var custom: Any? = null
        var viewModelStore: ViewModelStore? = null
    }

    private val contextAwareHelper = ContextAwareHelper()
    private val menuHostHelper = MenuHostHelper { invalidateMenu() }
    @Suppress("LeakingThis")
    private val savedStateRegistryController: SavedStateRegistryController =
        SavedStateRegistryController.create(this)

    // Lazily recreated from NonConfigurationInstances by val viewModelStore
    private var _viewModelStore: ViewModelStore? = null
    private val reportFullyDrawnExecutor = createFullyDrawnExecutor()
    override val fullyDrawnReporter by lazy {
        FullyDrawnReporter(reportFullyDrawnExecutor) { reportFullyDrawn() }
    }

    @LayoutRes private var contentLayoutId = 0
    private val nextLocalRequestCode = AtomicInteger()

    /**
     * Get the [ActivityResultRegistry] associated with this activity.
     *
     * @return the [ActivityResultRegistry]
     */
    final override val activityResultRegistry: ActivityResultRegistry =
        object : ActivityResultRegistry() {
            @Suppress("deprecation")
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                val activity = this@ComponentActivity

                // Immediate result path
                val synchronousResult = contract.getSynchronousResult(activity, input)
                if (synchronousResult != null) {
                    Handler(Looper.getMainLooper()).post {
                        dispatchResult(requestCode, synchronousResult.value)
                    }
                    return
                }

                // Start activity path
                val intent = contract.createIntent(activity, input)
                var optionsBundle: Bundle? = null
                // If there are any extras, we should defensively set the classLoader
                if (intent.extras != null && intent.extras!!.classLoader == null) {
                    intent.setExtrasClassLoader(activity.classLoader)
                }
                if (intent.hasExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE)) {
                    optionsBundle = intent.getBundleExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE)
                    intent.removeExtra(EXTRA_ACTIVITY_OPTIONS_BUNDLE)
                } else if (options != null) {
                    optionsBundle = options.toBundle()
                }
                if (ACTION_REQUEST_PERMISSIONS == intent.action) {
                    // requestPermissions path
                    var permissions = intent.getStringArrayExtra(EXTRA_PERMISSIONS)
                    if (permissions == null) {
                        permissions = arrayOfNulls(0)
                    }
                    ActivityCompat.requestPermissions(activity, permissions, requestCode)
                } else if (ACTION_INTENT_SENDER_REQUEST == intent.action) {
                    val request =
                        intent.getParcelableExtra<IntentSenderRequest>(EXTRA_INTENT_SENDER_REQUEST)
                    try {
                        // startIntentSenderForResult path
                        ActivityCompat.startIntentSenderForResult(
                            activity,
                            request!!.intentSender,
                            requestCode,
                            request.fillInIntent,
                            request.flagsMask,
                            request.flagsValues,
                            0,
                            optionsBundle,
                        )
                    } catch (e: SendIntentException) {
                        Handler(Looper.getMainLooper()).post {
                            dispatchResult(
                                requestCode,
                                RESULT_CANCELED,
                                Intent()
                                    .setAction(ACTION_INTENT_SENDER_REQUEST)
                                    .putExtra(EXTRA_SEND_INTENT_EXCEPTION, e),
                            )
                        }
                    }
                } else {
                    // startActivityForResult path
                    ActivityCompat.startActivityForResult(
                        activity,
                        intent,
                        requestCode,
                        optionsBundle,
                    )
                }
            }
        }
    private val onConfigurationChangedListeners = CopyOnWriteArrayList<Consumer<Configuration>>()
    private val onTrimMemoryListeners = CopyOnWriteArrayList<Consumer<Int>>()
    private val onNewIntentListeners = CopyOnWriteArrayList<Consumer<Intent>>()
    private val onMultiWindowModeChangedListeners =
        CopyOnWriteArrayList<Consumer<MultiWindowModeChangedInfo>>()
    private val onPictureInPictureModeChangedListeners =
        CopyOnWriteArrayList<Consumer<PictureInPictureModeChangedInfo>>()
    private val onUserLeaveHintListeners = CopyOnWriteArrayList<Runnable>()
    private var dispatchingOnMultiWindowModeChanged = false
    private var dispatchingOnPictureInPictureModeChanged = false

    // Inputs from `ComponentActivity.onBackPressed()`, which can get called when API < 33 or
    // when `android:enableOnBackInvokedCallback` is `false`.
    private val onBackPressedInput: DirectNavigationEventInput by lazy {
        val input = DirectNavigationEventInput()
        navigationEventDispatcher.addInput(input)
        input
    }

    /**
     * Default constructor for ComponentActivity. All Activities must have a default constructor for
     * API 27 and lower devices or when using the default [android.app.AppComponentFactory].
     */
    init {
        @Suppress("RedundantRequireNotNullCall", "LeakingThis")
        checkNotNull(lifecycle) {
            "getLifecycle() returned null in ComponentActivity's " +
                "constructor. Please make sure you are lazily constructing your Lifecycle " +
                "in the first call to getLifecycle() rather than relying on field " +
                "initialization."
        }
        @Suppress("LeakingThis")
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) {
                    window?.peekDecorView()?.cancelPendingInputEvents()
                }
            }
        )
        @Suppress("LeakingThis")
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    // Clear out the available context
                    contextAwareHelper.clearAvailableContext()
                    // And clear the ViewModelStore
                    if (!isChangingConfigurations) {
                        viewModelStore.clear()
                    }
                    reportFullyDrawnExecutor.activityDestroyed()
                }
            }
        )
        @Suppress("LeakingThis")
        lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    ensureViewModelStore()
                    lifecycle.removeObserver(this)
                }
            }
        )
        savedStateRegistryController.performAttach()
        enableSavedStateHandles()
        if (Build.VERSION.SDK_INT == 23) {
            @Suppress("LeakingThis") lifecycle.addObserver(ImmLeaksCleaner(this))
        }
        savedStateRegistry.registerSavedStateProvider(ACTIVITY_RESULT_TAG) {
            val outState = Bundle()
            activityResultRegistry.onSaveInstanceState(outState)
            outState
        }
        addOnContextAvailableListener {
            val savedInstanceState =
                savedStateRegistry.consumeRestoredStateForKey(ACTIVITY_RESULT_TAG)
            if (savedInstanceState != null) {
                activityResultRegistry.onRestoreInstanceState(savedInstanceState)
            }
        }
    }

    /**
     * Alternate constructor that can be used to provide a default layout that will be inflated as
     * part of `super.onCreate(savedInstanceState)`.
     *
     * This should generally be called from your constructor that takes no parameters, as is
     * required for API 27 and lower or when using the default [android.app.AppComponentFactory].
     */
    @ContentView
    constructor(@LayoutRes contentLayoutId: Int) : this() {
        this.contentLayoutId = contentLayoutId
    }

    /**
     * {@inheritDoc}
     *
     * If your ComponentActivity is annotated with [ContentView], this will call [setContentView]
     * for you.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        // Restore the Saved State first so that it is available to
        // OnContextAvailableListener instances
        savedStateRegistryController.performRestore(savedInstanceState)
        contextAwareHelper.dispatchOnContextAvailable(this)
        super.onCreate(savedInstanceState)
        ReportFragment.injectIfNeededIn(this)
        if (contentLayoutId != 0) {
            setContentView(contentLayoutId)
        }
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        if (lifecycle is LifecycleRegistry) {
            (lifecycle as LifecycleRegistry).currentState = Lifecycle.State.CREATED
        }
        super.onSaveInstanceState(outState)
        savedStateRegistryController.performSave(outState)
    }

    /**
     * Retain all appropriate non-config state. You can NOT override this yourself! Use a
     * [androidx.lifecycle.ViewModel] if you want to retain your own non config state.
     */
    @Suppress("deprecation")
    final override fun onRetainNonConfigurationInstance(): Any? {
        // Maintain backward compatibility.
        val custom = onRetainCustomNonConfigurationInstance()
        var viewModelStore = _viewModelStore
        if (viewModelStore == null) {
            // No one called getViewModelStore(), so see if there was an existing
            // ViewModelStore from our last NonConfigurationInstance
            val nc = lastNonConfigurationInstance as NonConfigurationInstances?
            if (nc != null) {
                viewModelStore = nc.viewModelStore
            }
        }
        if (viewModelStore == null && custom == null) {
            return null
        }
        val nci = NonConfigurationInstances()
        nci.custom = custom
        nci.viewModelStore = viewModelStore
        return nci
    }

    /**
     * Use this instead of [onRetainNonConfigurationInstance]. Retrieve later with
     * [lastCustomNonConfigurationInstance].
     */
    @Deprecated("Use a {@link androidx.lifecycle.ViewModel} to store non config state.")
    open fun onRetainCustomNonConfigurationInstance(): Any? {
        return null
    }

    @get:Deprecated("Use a {@link androidx.lifecycle.ViewModel} to store non config state.")
    open val lastCustomNonConfigurationInstance: Any?
        /** Return the value previously returned from [onRetainCustomNonConfigurationInstance]. */
        get() {
            val nc = lastNonConfigurationInstance as NonConfigurationInstances?
            return nc?.custom
        }

    override fun setContentView(@LayoutRes layoutResID: Int) {
        initializeViewTreeOwners()
        reportFullyDrawnExecutor.viewCreated(window.decorView)
        super.setContentView(layoutResID)
    }

    override fun setContentView(view: View?) {
        initializeViewTreeOwners()
        reportFullyDrawnExecutor.viewCreated(window.decorView)
        super.setContentView(view)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        initializeViewTreeOwners()
        reportFullyDrawnExecutor.viewCreated(window.decorView)
        super.setContentView(view, params)
    }

    override fun addContentView(view: View?, params: ViewGroup.LayoutParams?) {
        initializeViewTreeOwners()
        reportFullyDrawnExecutor.viewCreated(window.decorView)
        super.addContentView(view, params)
    }

    /**
     * Sets the view tree owners before setting the content view so that the inflation process and
     * attach listeners will see them already present.
     */
    @CallSuper
    open fun initializeViewTreeOwners() {
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeViewModelStoreOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)
        window.decorView.setViewTreeOnBackPressedDispatcherOwner(this)
        window.decorView.setViewTreeFullyDrawnReporterOwner(this)
        window.decorView.setViewTreeNavigationEventDispatcherOwner(this)
    }

    override fun peekAvailableContext(): Context? {
        return contextAwareHelper.peekAvailableContext()
    }

    /**
     * {@inheritDoc}
     *
     * Any listener added here will receive a callback as part of `super.onCreate()`, but
     * importantly **before** any other logic is done (including calling through to the framework
     * [Activity.onCreate] with the exception of restoring the state of the [savedStateRegistry] for
     * use in your listener.
     */
    final override fun addOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.addOnContextAvailableListener(listener)
    }

    final override fun removeOnContextAvailableListener(listener: OnContextAvailableListener) {
        contextAwareHelper.removeOnContextAvailableListener(listener)
    }

    override fun onPreparePanel(featureId: Int, view: View?, menu: Menu): Boolean {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            super.onPreparePanel(featureId, view, menu)
            menuHostHelper.onPrepareMenu(menu)
        }
        return true
    }

    override fun onCreatePanelMenu(featureId: Int, menu: Menu): Boolean {
        if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            super.onCreatePanelMenu(featureId, menu)
            menuHostHelper.onCreateMenu(menu, getMenuInflater())
        }
        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (super.onMenuItemSelected(featureId, item)) {
            return true
        }
        return if (featureId == Window.FEATURE_OPTIONS_PANEL) {
            menuHostHelper.onMenuItemSelected(item)
        } else false
    }

    override fun onPanelClosed(featureId: Int, menu: Menu) {
        menuHostHelper.onMenuClosed(menu)
        super.onPanelClosed(featureId, menu)
    }

    override fun addMenuProvider(provider: MenuProvider) {
        menuHostHelper.addMenuProvider(provider)
    }

    override fun addMenuProvider(provider: MenuProvider, owner: LifecycleOwner) {
        menuHostHelper.addMenuProvider(provider, owner)
    }

    @SuppressLint("LambdaLast")
    override fun addMenuProvider(
        provider: MenuProvider,
        owner: LifecycleOwner,
        state: Lifecycle.State,
    ) {
        menuHostHelper.addMenuProvider(provider, owner, state)
    }

    override fun removeMenuProvider(provider: MenuProvider) {
        menuHostHelper.removeMenuProvider(provider)
    }

    override fun invalidateMenu() {
        invalidateOptionsMenu()
    }

    /**
     * {@inheritDoc}
     *
     * Overriding this method is no longer supported and this method will be made `final` in a
     * future version of ComponentActivity. If you do override this method, you *must*:
     * 1. Return an instance of [LifecycleRegistry]
     * 1. Lazily initialize your LifecycleRegistry object when this is first called.
     *
     * Note that this method will be called in the super classes' constructor, before any field
     * initialization or object state creation is complete.
     */
    override val lifecycle: Lifecycle
        get() = super.lifecycle

    override val viewModelStore: ViewModelStore
        /**
         * Returns the [ViewModelStore] associated with this activity
         *
         * Overriding this method is no longer supported and this method will be made `final` in a
         * future version of ComponentActivity.
         *
         * @return a [ViewModelStore]
         * @throws IllegalStateException if called before the Activity is attached to the
         *   Application instance i.e., before onCreate()
         */
        get() {
            checkNotNull(application) {
                ("Your activity is not yet attached to the " +
                    "Application instance. You can't request ViewModel before onCreate call.")
            }
            ensureViewModelStore()
            return _viewModelStore!!
        }

    private fun ensureViewModelStore() {
        if (_viewModelStore == null) {
            val nc = lastNonConfigurationInstance as NonConfigurationInstances?
            if (nc != null) {
                // Restore the ViewModelStore from NonConfigurationInstances
                _viewModelStore = nc.viewModelStore
            }
            if (_viewModelStore == null) {
                _viewModelStore = ViewModelStore()
            }
        }
    }

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        SavedStateViewModelFactory(application, this, if (intent != null) intent.extras else null)
    }

    @get:CallSuper
    override val defaultViewModelCreationExtras: CreationExtras
        /**
         * {@inheritDoc}
         *
         * The extras of [getIntent] when this is first called will be used as the defaults to any
         * [androidx.lifecycle.SavedStateHandle] passed to a view model created using this extra.
         */
        get() {
            val extras = MutableCreationExtras()
            if (application != null) {
                extras[APPLICATION_KEY] = application
            }
            extras[SAVED_STATE_REGISTRY_OWNER_KEY] = this
            extras[VIEW_MODEL_STORE_OWNER_KEY] = this
            val intentExtras = intent?.extras
            if (intentExtras != null) {
                extras[DEFAULT_ARGS_KEY] = intentExtras
            }
            return extras
        }

    /**
     * Called when the activity has detected the user's press of the back key. The
     * [onBackPressedDispatcher] will be given a chance to handle the back button before the default
     * behavior of [android.app.Activity.onBackPressed] is invoked.
     *
     * @see onBackPressedDispatcher
     */
    @MainThread
    @CallSuper
    @Deprecated(
        """This method has been deprecated in favor of using the
      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.
      The OnBackPressedDispatcher controls how back button events are dispatched
      to one or more {@link OnBackPressedCallback} objects."""
    )
    override fun onBackPressed() {
        onBackPressedInput.complete()
    }

    /**
     * Retrieve the [OnBackPressedDispatcher] that will be triggered when [onBackPressed] is called.
     *
     * @return The [OnBackPressedDispatcher] associated with this ComponentActivity.
     */
    final override val onBackPressedDispatcher: OnBackPressedDispatcher by lazy {
        OnBackPressedDispatcher(
                fallbackOnBackPressed = {
                    // Calling onBackPressed() on an Activity with its state saved can cause an
                    // error on devices on API levels before 26. We catch that specific error
                    // and throw all others.
                    try {
                        @Suppress("DEPRECATION") super@ComponentActivity.onBackPressed()
                    } catch (e: IllegalStateException) {
                        if (e.message != "Can not perform this action after onSaveInstanceState") {
                            throw e
                        }
                    } catch (e: NullPointerException) {
                        if (
                            e.message !=
                                "Attempt to invoke virtual method 'android.os.Handler " +
                                    "android.app.FragmentHostCallback.getHandler()' on a " +
                                    "null object reference"
                        ) {
                            throw e
                        }
                    }
                }
            )
            .also { dispatcher ->
                if (Build.VERSION.SDK_INT >= 33) {
                    if (Looper.myLooper() != Looper.getMainLooper()) {
                        Handler(Looper.getMainLooper()).post {
                            addObserverForBackInvoker(dispatcher)
                        }
                    } else {
                        addObserverForBackInvoker(dispatcher)
                    }
                }
            }
    }

    /**
     * Lazily provides a [NavigationEventDispatcher] for back navigation handling, including support
     * for predictive back gestures introduced in Android 13 (API 33+).
     *
     * This dispatcher acts as the central point for back navigation events. When a navigation event
     * occurs (e.g., a back gesture), it safely invokes [ComponentActivity.onBackPressed].
     */
    override val navigationEventDispatcher: NavigationEventDispatcher
        get() = onBackPressedDispatcher.eventDispatcher

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addObserverForBackInvoker(dispatcher: OnBackPressedDispatcher) {
        lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_CREATE) {
                    dispatcher.setOnBackInvokedDispatcher(getOnBackInvokedDispatcher())
                }
            }
        )
    }

    final override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    /** {@inheritDoc} */
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}."""
    )
    override fun startActivityForResult(intent: Intent, requestCode: Int) {
        super.startActivityForResult(intent, requestCode)
    }

    /** {@inheritDoc} */
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      passing in a {@link StartActivityForResult} object for the {@link ActivityResultContract}."""
    )
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: Bundle?) {
        super.startActivityForResult(intent, requestCode, options)
    }

    /** {@inheritDoc} */
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      passing in a {@link StartIntentSenderForResult} object for the
      {@link ActivityResultContract}."""
    )
    @Throws(SendIntentException::class)
    override fun startIntentSenderForResult(
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
    ) {
        super.startIntentSenderForResult(
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
        )
    }

    /** {@inheritDoc} */
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      passing in a {@link StartIntentSenderForResult} object for the
      {@link ActivityResultContract}."""
    )
    @Throws(SendIntentException::class)
    override fun startIntentSenderForResult(
        intent: IntentSender,
        requestCode: Int,
        fillInIntent: Intent?,
        flagsMask: Int,
        flagsValues: Int,
        extraFlags: Int,
        options: Bundle?,
    ) {
        super.startIntentSenderForResult(
            intent,
            requestCode,
            fillInIntent,
            flagsMask,
            flagsValues,
            extraFlags,
            options,
        )
    }

    /** {@inheritDoc} */
    @CallSuper
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}
      with the appropriate {@link ActivityResultContract} and handling the result in the
      {@link ActivityResultCallback#onActivityResult(Object) callback}."""
    )
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!activityResultRegistry.dispatchResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /** {@inheritDoc} */
    @CallSuper
    @Deprecated(
        """This method has been deprecated in favor of using the Activity Result API
      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt
      contracts for common intents available in
      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for
      testing, and allow receiving results in separate, testable classes independent from your
      activity. Use
      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing
      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and
      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}."""
    )
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        if (
            !activityResultRegistry.dispatchResult(
                requestCode,
                RESULT_OK,
                Intent()
                    .putExtra(EXTRA_PERMISSIONS, permissions)
                    .putExtra(EXTRA_PERMISSION_GRANT_RESULTS, grantResults),
            )
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    final override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        registry: ActivityResultRegistry,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        return registry.register(
            "activity_rq#" + nextLocalRequestCode.getAndIncrement(),
            this,
            contract,
            callback,
        )
    }

    final override fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: ActivityResultCallback<O>,
    ): ActivityResultLauncher<I> {
        return registerForActivityResult(contract, activityResultRegistry, callback)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnConfigurationChangedListener].
     */
    @CallSuper
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        for (listener in onConfigurationChangedListeners) {
            listener.accept(newConfig)
        }
    }

    final override fun addOnConfigurationChangedListener(listener: Consumer<Configuration>) {
        onConfigurationChangedListeners.add(listener)
    }

    final override fun removeOnConfigurationChangedListener(listener: Consumer<Configuration>) {
        onConfigurationChangedListeners.remove(listener)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnTrimMemoryListener].
     */
    @CallSuper
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        for (listener in onTrimMemoryListeners) {
            listener.accept(level)
        }
    }

    final override fun addOnTrimMemoryListener(listener: Consumer<Int>) {
        onTrimMemoryListeners.add(listener)
    }

    final override fun removeOnTrimMemoryListener(listener: Consumer<Int>) {
        onTrimMemoryListeners.remove(listener)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnNewIntentListener].
     */
    @CallSuper
    override fun onNewIntent(@Suppress("InvalidNullabilityOverride") intent: Intent) {
        super.onNewIntent(intent)
        for (listener in onNewIntentListeners) {
            listener.accept(intent)
        }
    }

    final override fun addOnNewIntentListener(listener: Consumer<Intent>) {
        onNewIntentListeners.add(listener)
    }

    final override fun removeOnNewIntentListener(listener: Consumer<Intent>) {
        onNewIntentListeners.remove(listener)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnMultiWindowModeChangedListener].
     */
    @Deprecated("Deprecated in android.app.Activity")
    @CallSuper
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean) {
        // We specifically do not call super.onMultiWindowModeChanged() to avoid
        // crashing when this method is manually called prior to API 24 (which is
        // when this method was added to the framework)
        if (dispatchingOnMultiWindowModeChanged) {
            return
        }
        for (listener in onMultiWindowModeChangedListeners) {
            listener.accept(MultiWindowModeChangedInfo(isInMultiWindowMode))
        }
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnMultiWindowModeChangedListener].
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @CallSuper
    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        dispatchingOnMultiWindowModeChanged = true
        try {
            // We can unconditionally call super.onMultiWindowModeChanged() here because this
            // function is marked with RequiresApi, meaning we are always on an API level
            // where this call is valid.
            super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        } finally {
            dispatchingOnMultiWindowModeChanged = false
        }
        for (listener in onMultiWindowModeChangedListeners) {
            listener.accept(MultiWindowModeChangedInfo(isInMultiWindowMode, newConfig))
        }
    }

    final override fun addOnMultiWindowModeChangedListener(
        listener: Consumer<MultiWindowModeChangedInfo>
    ) {
        onMultiWindowModeChangedListeners.add(listener)
    }

    final override fun removeOnMultiWindowModeChangedListener(
        listener: Consumer<MultiWindowModeChangedInfo>
    ) {
        onMultiWindowModeChangedListeners.remove(listener)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnPictureInPictureModeChangedListener].
     */
    @Deprecated("Deprecated in android.app.Activity")
    @CallSuper
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        // We specifically do not call super.onPictureInPictureModeChanged() to avoid
        // crashing when this method is manually called prior to API 24 (which is
        // when this method was added to the framework)
        if (dispatchingOnPictureInPictureModeChanged) {
            return
        }
        for (listener in onPictureInPictureModeChangedListeners) {
            listener.accept(PictureInPictureModeChangedInfo(isInPictureInPictureMode))
        }
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnPictureInPictureModeChangedListener].
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @CallSuper
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        dispatchingOnPictureInPictureModeChanged = true
        try {
            // We can unconditionally call super.onPictureInPictureModeChanged() here because
            // this function is marked with RequiresApi, meaning we are always on an API level
            // where this call is valid.
            super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        } finally {
            dispatchingOnPictureInPictureModeChanged = false
        }
        for (listener in onPictureInPictureModeChangedListeners) {
            listener.accept(PictureInPictureModeChangedInfo(isInPictureInPictureMode, newConfig))
        }
    }

    final override fun addOnPictureInPictureModeChangedListener(
        listener: Consumer<PictureInPictureModeChangedInfo>
    ) {
        onPictureInPictureModeChangedListeners.add(listener)
    }

    final override fun removeOnPictureInPictureModeChangedListener(
        listener: Consumer<PictureInPictureModeChangedInfo>
    ) {
        onPictureInPictureModeChangedListeners.remove(listener)
    }

    /**
     * {@inheritDoc}
     *
     * Dispatches this call to all listeners added via [addOnUserLeaveHintListener].
     */
    @CallSuper
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        for (listener in onUserLeaveHintListeners) {
            listener.run()
        }
    }

    final override fun addOnUserLeaveHintListener(listener: Runnable) {
        onUserLeaveHintListeners.add(listener)
    }

    final override fun removeOnUserLeaveHintListener(listener: Runnable) {
        onUserLeaveHintListeners.remove(listener)
    }

    override fun reportFullyDrawn() {
        try {
            if (Trace.isEnabled()) {
                Trace.beginSection("reportFullyDrawn() for ComponentActivity")
            }
            super.reportFullyDrawn()
            // Activity.reportFullyDrawn() was added in API 19, so we can't call super
            // prior to that, but we still need to update our FullyLoadedReporter's state
            fullyDrawnReporter.fullyDrawnReported()
        } finally {
            Trace.endSection()
        }
    }

    private fun createFullyDrawnExecutor(): ReportFullyDrawnExecutor =
        ReportFullyDrawnExecutorImpl()

    private interface ReportFullyDrawnExecutor : Executor {
        fun viewCreated(view: View)

        fun activityDestroyed()
    }

    private inner class ReportFullyDrawnExecutorImpl :
        ReportFullyDrawnExecutor, OnDrawListener, Runnable {
        val endWatchTimeMillis = SystemClock.uptimeMillis() + 10000
        var currentRunnable: Runnable? = null
        var onDrawScheduled = false

        override fun viewCreated(view: View) {
            if (!onDrawScheduled) {
                onDrawScheduled = true
                view.getViewTreeObserver().addOnDrawListener(this)
            }
        }

        override fun activityDestroyed() {
            window.decorView.removeCallbacks(this)
            window.decorView.getViewTreeObserver().removeOnDrawListener(this)
        }

        /**
         * Called when we want to execute runnable that might call
         * [ComponentActivity.reportFullyDrawn].
         *
         * @param runnable The call to potentially execute reportFullyDrawn().
         */
        override fun execute(runnable: Runnable) {
            currentRunnable = runnable
            val decorView = window.decorView
            if (onDrawScheduled) {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    decorView.invalidate()
                } else {
                    decorView.postInvalidate()
                }
            } else {
                // We've already gotten past the 10 second timeout and dropped the
                // OnPreDrawListener, so we just run on the next frame.
                decorView.postOnAnimation {
                    if (currentRunnable != null) {
                        currentRunnable!!.run()
                        currentRunnable = null
                    }
                }
            }
        }

        override fun onDraw() {
            val runnable = currentRunnable
            if (runnable != null) {
                runnable.run()
                currentRunnable = null
                if (fullyDrawnReporter.isFullyDrawnReported) {
                    onDrawScheduled = false
                    window.decorView.post(this) // remove the listener
                }
            } else if (SystemClock.uptimeMillis() > endWatchTimeMillis) {
                // We've gone 10 seconds without calling reportFullyDrawn().
                // We'll just stop doing this check to avoid unnecessary overhead.
                onDrawScheduled = false
                window.decorView.post(this) // remove the listener
            }
        }

        /**
         * Called when we want to remove the OnDrawListener. OnDrawListener can't be removed from
         * within the onDraw() method.
         */
        override fun run() {
            window.decorView.getViewTreeObserver().removeOnDrawListener(this)
        }
    }

    private companion object {
        private const val ACTIVITY_RESULT_TAG = "android:support:activity-result"
    }
}

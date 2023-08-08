/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi

import android.animation.AnimationHandler
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.os.Handler_Delegate
import android.os.SystemClock_Delegate
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.BridgeInflater
import android.view.Choreographer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.LayoutRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.platform.AndroidUiDispatcher
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.cash.paparazzi.agent.AgentTestRule
import app.cash.paparazzi.agent.InterceptorRegistrar
import app.cash.paparazzi.internal.ChoreographerDelegateInterceptor
import app.cash.paparazzi.internal.EditModeInterceptor
import app.cash.paparazzi.internal.IInputMethodManagerInterceptor
import app.cash.paparazzi.internal.ImageUtils
import app.cash.paparazzi.internal.MatrixMatrixMultiplicationInterceptor
import app.cash.paparazzi.internal.MatrixVectorMultiplicationInterceptor
import app.cash.paparazzi.internal.PaparazziCallback
import app.cash.paparazzi.internal.PaparazziLogger
import app.cash.paparazzi.internal.Renderer
import app.cash.paparazzi.internal.ResourcesInterceptor
import app.cash.paparazzi.internal.ServiceManagerInterceptor
import app.cash.paparazzi.internal.SessionParamsBuilder
import app.cash.paparazzi.internal.parsers.LayoutPullParser
import com.android.ide.common.rendering.api.RenderSession
import com.android.ide.common.rendering.api.Result
import com.android.ide.common.rendering.api.Result.Status.ERROR_UNKNOWN
import com.android.ide.common.rendering.api.SessionParams
import com.android.ide.common.rendering.api.SessionParams.RenderingMode
import com.android.internal.lang.System_Delegate
import com.android.layoutlib.bridge.Bridge
import com.android.layoutlib.bridge.Bridge.cleanupThread
import com.android.layoutlib.bridge.Bridge.prepareThread
import com.android.layoutlib.bridge.BridgeRenderSession
import com.android.layoutlib.bridge.impl.RenderAction
import com.android.layoutlib.bridge.impl.RenderSessionImpl
import java.awt.image.BufferedImage
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.coroutines.ContinuationInterceptor
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class Paparazzi @JvmOverloads constructor(
  private val environment: Environment = detectEnvironment(),
  private val deviceConfig: DeviceConfig = DeviceConfig.NEXUS_5,
  private val theme: String = "android:Theme.Material.NoActionBar.Fullscreen",
  private val renderingMode: RenderingMode = RenderingMode.NORMAL,
  private val appCompatEnabled: Boolean = true,
  private val maxPercentDifference: Double = 0.1,
  private val snapshotHandler: SnapshotHandler = determineHandler(maxPercentDifference),
  private val renderExtensions: Set<RenderExtension> = setOf()
) : TestRule {
  private val logger = PaparazziLogger()
  private lateinit var renderSession: RenderSessionImpl
  private lateinit var bridgeRenderSession: RenderSession
  private var testName: TestName? = null

  val layoutInflater: LayoutInflater
    get() = RenderAction.getCurrentContext().getSystemService("layout_inflater") as BridgeInflater

  val resources: Resources
    get() = RenderAction.getCurrentContext().resources

  val context: Context
    get() = RenderAction.getCurrentContext()

  /**
   * The root layout that test views will be placed into. The FrameLayout is dynamically set to
   * `wrap_content` if the `renderMode` is `RenderingMode.SizeAction.SHRINK` in the appropriate
   * direction, otherwise it is set to `match_parent`.
   */
  private val contentRoot = """
        |<?xml version="1.0" encoding="utf-8"?>
        |<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
        |              android:layout_width="${renderingMode.horizAction.toAttrValue()}"
        |              android:layout_height="${renderingMode.vertAction.toAttrValue()}"/>
  """.trimMargin()

  private fun RenderingMode.SizeAction.toAttrValue() =
    if (this == RenderingMode.SizeAction.SHRINK) "wrap_content" else "match_parent"

  override fun apply(
    base: Statement,
    description: Description
  ): Statement {
    val statement = object : Statement() {
      override fun evaluate() {
        prepare(description)
        try {
          base.evaluate()
        } finally {
          close()
          logger.assertNoErrors()
        }
      }
    }

    return if (!isInitialized) {
      registerFontLookupInterceptionIfResourceCompatDetected()
      registerViewEditModeInterception()
      registerMatrixMultiplyInterception()
      registerChoreographerDelegateInterception()
      registerServiceManagerInterception()
      registerIInputMethodManagerInterception()

      val outerRule = AgentTestRule()
      outerRule.apply(statement, description)
    } else {
      statement
    }
  }

  fun prepare(description: Description) {
    forcePlatformSdkVersion(environment.compileSdkVersion)

    val layoutlibCallback = PaparazziCallback(logger, environment.resourcePackageNames)
    layoutlibCallback.initResources()

    testName = description.toTestName()

    if (!isInitialized) {
      renderer = Renderer(environment, layoutlibCallback, logger, maxPercentDifference)
      sessionParamsBuilder = renderer.prepare()
    }

    sessionParamsBuilder = sessionParamsBuilder
      .copy(
        layoutPullParser = LayoutPullParser.createFromString(contentRoot),
        deviceConfig = deviceConfig,
        renderingMode = renderingMode
      )
      .withTheme(theme)

    val sessionParams = sessionParamsBuilder.build()
    renderSession = createRenderSession(sessionParams)
    prepareThread()
    renderSession.init(sessionParams.timeout)
    Bitmap.setDefaultDensity(DisplayMetrics.DENSITY_DEVICE_STABLE)

    // requires LayoutInflater to be created, which is a side-effect of RenderSessionImpl.init()
    if (appCompatEnabled) {
      initializeAppCompatIfPresent()
    }

    bridgeRenderSession = createBridgeSession(renderSession, renderSession.inflate())
  }

  fun close() {
    testName = null
    renderSession.release()
    bridgeRenderSession.dispose()
    cleanupThread()
    snapshotHandler.close()

    renderer.dumpDelegates()
  }

  @Suppress("UNCHECKED_CAST")
  fun <V : View> inflate(@LayoutRes layoutId: Int): V =
    layoutInflater.inflate(layoutId, null) as V

  fun snapshot(name: String? = null, composable: @Composable () -> Unit) {
    val hostView = ComposeView(context)
    // During onAttachedToWindow, AbstractComposeView will attempt to resolve its parent's
    // CompositionContext, which requires first finding the "content view", then using that to
    // find a root view with a ViewTreeLifecycleOwner
    val parent = FrameLayout(context).apply { id = android.R.id.content }
    parent.addView(
      hostView,
      renderingMode.horizAction.toLayoutParams(),
      renderingMode.vertAction.toLayoutParams()
    )
    PaparazziComposeOwner.register(parent)
    hostView.setContent(composable)

    try {
      snapshot(parent, name)
    } finally {
      forceReleaseComposeReferenceLeaks()
    }
  }

  private fun RenderingMode.SizeAction.toLayoutParams() =
    if (this == RenderingMode.SizeAction.SHRINK) {
      LayoutParams.WRAP_CONTENT
    } else {
      LayoutParams.MATCH_PARENT
    }

  @JvmOverloads
  fun snapshot(view: View, name: String? = null) {
    takeSnapshots(view, name, 0, -1, 1)
  }

  @JvmOverloads
  fun gif(
    view: View,
    name: String? = null,
    start: Long = 0L,
    end: Long = 500L,
    fps: Int = 30
  ) {
    // Add one to the frame count so we get the last frame. Otherwise a 1 second, 60 FPS animation
    // our 60th frame will be at time 983 ms, and we want our last frame to be 1,000 ms. This gets
    // us 61 frames for a 1 second animation, 121 frames for a 2 second animation, etc.
    val durationMillis = (end - start).toInt()
    val frameCount = (durationMillis * fps) / 1000 + 1
    val startNanos = TimeUnit.MILLISECONDS.toNanos(start)
    takeSnapshots(view, name, startNanos, fps, frameCount)
  }

  fun unsafeUpdateConfig(
    deviceConfig: DeviceConfig? = null,
    theme: String? = null,
    renderingMode: RenderingMode? = null
  ) {
    require(deviceConfig != null || theme != null || renderingMode != null) {
      "Calling unsafeUpdateConfig requires at least one non-null argument."
    }

    renderSession.release()
    bridgeRenderSession.dispose()
    cleanupThread()

    sessionParamsBuilder = sessionParamsBuilder
      .copy(
        // Required to reset underlying parser stream
        layoutPullParser = LayoutPullParser.createFromString(contentRoot)
      )

    if (deviceConfig != null) {
      sessionParamsBuilder = sessionParamsBuilder.copy(deviceConfig = deviceConfig)
    }

    if (theme != null) {
      sessionParamsBuilder = sessionParamsBuilder.withTheme(theme)
    }

    if (renderingMode != null) {
      sessionParamsBuilder = sessionParamsBuilder.copy(renderingMode = renderingMode)
    }

    val sessionParams = sessionParamsBuilder.build()
    renderSession = createRenderSession(sessionParams)
    prepareThread()
    renderSession.init(sessionParams.timeout)
    Bitmap.setDefaultDensity(DisplayMetrics.DENSITY_DEVICE_STABLE)
    bridgeRenderSession = createBridgeSession(renderSession, renderSession.inflate())
  }

  private fun takeSnapshots(
    view: View,
    name: String?,
    startNanos: Long,
    fps: Int,
    frameCount: Int
  ) {
    val snapshot = Snapshot(name, testName!!, Date())

    val frameHandler = snapshotHandler.newFrameHandler(snapshot, frameCount, fps)
    frameHandler.use {
      val viewGroup = bridgeRenderSession.rootViews[0].viewObject as ViewGroup
      val modifiedView = renderExtensions.fold(view) { view, renderExtension ->
        renderExtension.renderView(view)
      }

      System_Delegate.setBootTimeNanos(0L)
      try {
        withTime(0L) {
          // Initialize the choreographer at time=0.
        }

        viewGroup.addView(modifiedView)
        for (frame in 0 until frameCount) {
          val nowNanos = (startNanos + (frame * 1_000_000_000.0 / fps)).toLong()
          withTime(nowNanos) {
            val result = renderSession.render(true)
            if (result.status == ERROR_UNKNOWN) {
              throw result.exception
            }

            val image = bridgeRenderSession.image
            frameHandler.handle(scaleImage(image))
          }
        }
      } finally {
        viewGroup.removeView(modifiedView)
        AnimationHandler.sAnimatorHandler.set(null)
      }
    }
  }

  private fun withTime(
    timeNanos: Long,
    block: () -> Unit
  ) {
    val frameNanos = TIME_OFFSET_NANOS + timeNanos

    // Execute the block at the requested time.
    System_Delegate.setNanosTime(frameNanos)

    val choreographer = Choreographer.getInstance()
    val areCallbacksRunningField = choreographer::class.java.getDeclaredField("mCallbacksRunning")
    areCallbacksRunningField.isAccessible = true

    try {
      areCallbacksRunningField.setBoolean(choreographer, true)

      // https://android.googlesource.com/platform/frameworks/layoutlib/+/d58aa4703369e109b24419548f38b422d5a44738/bridge/src/com/android/layoutlib/bridge/BridgeRenderSession.java#171
      // BridgeRenderSession.executeCallbacks aggressively tears down the main Looper and BridgeContext, so we call the static delegates ourselves.
      Handler_Delegate.executeCallbacks()
      val currentTimeMs = SystemClock_Delegate.uptimeMillis()
      val choreographerCallbacks =
        RenderAction.getCurrentContext().sessionInteractiveData.choreographerCallbacks
      choreographerCallbacks.execute(currentTimeMs, Bridge.getLog())

      block()
    } catch (e: Throwable) {
      Bridge.getLog().error("broken", "Failed executing Choreographer#doFrame", e, null, null)
      throw e
    } finally {
      areCallbacksRunningField.setBoolean(choreographer, false)
    }
  }

  private fun createRenderSession(sessionParams: SessionParams): RenderSessionImpl {
    val renderSession = RenderSessionImpl(sessionParams)
    renderSession.setElapsedFrameTimeNanos(0L)
    RenderSessionImpl::class.java
      .getDeclaredField("mFirstFrameExecuted")
      .apply {
        isAccessible = true
        set(renderSession, true)
      }
    return renderSession
  }

  private fun createBridgeSession(
    renderSession: RenderSessionImpl,
    result: Result
  ): BridgeRenderSession {
    try {
      val bridgeSessionClass = Class.forName("com.android.layoutlib.bridge.BridgeRenderSession")
      val constructor =
        bridgeSessionClass.getDeclaredConstructor(RenderSessionImpl::class.java, Result::class.java)
      constructor.isAccessible = true
      return constructor.newInstance(renderSession, result) as BridgeRenderSession
    } catch (e: Exception) {
      throw RuntimeException(e)
    }
  }

  private fun scaleImage(image: BufferedImage): BufferedImage {
    val scale = ImageUtils.getThumbnailScale(image)
    // Only scale images down so we don't waste storage space enlarging smaller layouts.
    return if (scale < 1f) ImageUtils.scale(image, scale, scale) else image
  }

  private fun Description.toTestName(): TestName {
    val fullQualifiedName = className
    val packageName = fullQualifiedName.substringBeforeLast('.', missingDelimiterValue = "")
    val className = fullQualifiedName.substringAfterLast('.')
    return TestName(packageName, className, methodName)
  }

  private fun forcePlatformSdkVersion(compileSdkVersion: Int) {
    val buildVersionClass = try {
      Paparazzi::class.java.classLoader.loadClass("android.os.Build\$VERSION")
    } catch (e: ClassNotFoundException) {
      // Project unit tests don't load Android platform code
      return
    }
    buildVersionClass
      .getFieldReflectively("SDK_INT")
      .setStaticValue(compileSdkVersion)
  }

  private fun initializeAppCompatIfPresent() {
    lateinit var appCompatDelegateClass: Class<*>
    try {
      // See androidx.appcompat.widget.AppCompatDrawableManager#preload()
      val appCompatDrawableManagerClass =
        Class.forName("androidx.appcompat.widget.AppCompatDrawableManager")
      val preloadMethod = appCompatDrawableManagerClass.getMethod("preload")
      preloadMethod.invoke(null)

      appCompatDelegateClass = Class.forName("androidx.appcompat.app.AppCompatDelegate")
    } catch (e: ClassNotFoundException) {
      logger.verbose("AppCompat not found on classpath")
      return
    }

    // See androidx.appcompat.app.AppCompatDelegateImpl#installViewFactory()
    if (layoutInflater.factory == null) {
      layoutInflater.factory2 = object : LayoutInflater.Factory2 {
        override fun onCreateView(
          parent: View?,
          name: String,
          context: Context,
          attrs: AttributeSet
        ): View? {
          val appCompatViewInflaterClass =
            Class.forName("androidx.appcompat.app.AppCompatViewInflater")

          val createViewMethod = appCompatViewInflaterClass
            .getDeclaredMethod(
              "createView",
              View::class.java,
              String::class.java,
              Context::class.java,
              AttributeSet::class.java,
              Boolean::class.javaPrimitiveType,
              Boolean::class.javaPrimitiveType,
              Boolean::class.javaPrimitiveType,
              Boolean::class.javaPrimitiveType
            )
            .apply { isAccessible = true }

          val inheritContext = true
          val readAndroidTheme = true
          val readAppTheme = true
          val wrapContext = true

          val newAppCompatViewInflaterInstance = appCompatViewInflaterClass
            .getConstructor()
            .newInstance()

          return createViewMethod.invoke(
            newAppCompatViewInflaterInstance, parent, name, context, attrs,
            inheritContext, readAndroidTheme, readAppTheme, wrapContext
          ) as View?
        }

        override fun onCreateView(
          name: String,
          context: Context,
          attrs: AttributeSet
        ): View? = onCreateView(null, name, context, attrs)
      }
    } else {
      if (!appCompatDelegateClass.isAssignableFrom(layoutInflater.factory2::class.java)) {
        throw IllegalStateException(
          "The LayoutInflater already has a Factory installed so we can not install AppCompat's"
        )
      }
    }
  }

  /**
   * Current workaround for supporting custom fonts when constructing views in code. This check
   * may be used or expanded to support other cases requiring similar method interception
   * techniques.
   *
   * See:
   * https://github.com/cashapp/paparazzi/issues/119
   * https://issuetracker.google.com/issues/156065472
   */
  private fun registerFontLookupInterceptionIfResourceCompatDetected() {
    try {
      val resourcesCompatClass = Class.forName("androidx.core.content.res.ResourcesCompat")
      InterceptorRegistrar.addMethodInterceptor(
        resourcesCompatClass,
        "getFont",
        ResourcesInterceptor::class.java
      )
    } catch (e: ClassNotFoundException) {
      logger.verbose("ResourceCompat not found on classpath")
    }
  }

  private fun registerServiceManagerInterception() {
    val serviceManager = Class.forName("android.os.ServiceManager")
    InterceptorRegistrar.addMethodInterceptor(
      serviceManager,
      "getServiceOrThrow",
      ServiceManagerInterceptor::class.java
    )
  }

  private fun registerIInputMethodManagerInterception() {
    val iimm = Class.forName("com.android.internal.view.IInputMethodManager\$Stub")
    InterceptorRegistrar.addMethodInterceptor(
      iimm,
      "asInterface",
      IInputMethodManagerInterceptor::class.java
    )
  }

  private fun registerViewEditModeInterception() {
    val viewClass = Class.forName("android.view.View")
    InterceptorRegistrar.addMethodInterceptor(
      viewClass,
      "isInEditMode",
      EditModeInterceptor::class.java
    )
  }

  private fun registerMatrixMultiplyInterception() {
    val matrixClass = Class.forName("android.opengl.Matrix")
    InterceptorRegistrar.addMethodInterceptors(
      matrixClass,
      setOf(
        "multiplyMM" to MatrixMatrixMultiplicationInterceptor::class.java,
        "multiplyMV" to MatrixVectorMultiplicationInterceptor::class.java
      )
    )
  }

  private fun registerChoreographerDelegateInterception() {
    val choreographerDelegateClass = Class.forName("android.view.Choreographer_Delegate")
    InterceptorRegistrar.addMethodInterceptor(
      choreographerDelegateClass,
      "getFrameTimeNanos",
      ChoreographerDelegateInterceptor::class.java
    )
  }

  private fun forceReleaseComposeReferenceLeaks() {
    val snapshotClass = Class.forName("androidx.compose.runtime.snapshots.SnapshotKt")
    val applyObservers = snapshotClass
      .getDeclaredField("applyObservers")
      .apply { isAccessible = true }
      .get(null) as MutableList<*>
    val applyObserver = applyObservers.getOrNull(0)
    if (applyObserver != null) {
      val recomposer = applyObserver.javaClass
        .getDeclaredField("this\$0")
        .apply { isAccessible = true }
        .get(applyObserver) as Recomposer
      val compositionInvalidations = recomposer.javaClass
        .getDeclaredField("compositionInvalidations")
        .apply { isAccessible = true }
        .get(recomposer) as MutableCollection<*>
      val snapshotInvalidations = recomposer.javaClass
        .getDeclaredField("snapshotInvalidations")
        .apply { isAccessible = true }
        .get(recomposer)
      compositionInvalidations.clear()
      applyObservers.clear()

      if (snapshotInvalidations is MutableCollection<*>) {
        snapshotInvalidations.clear()
      } else {
        // backed by IdentityArraySet
        snapshotInvalidations.javaClass
          .getDeclaredMethod("clear")
          .apply { isAccessible = true }
          .invoke(snapshotInvalidations)
      }
    }

    val dispatcher =
      AndroidUiDispatcher.CurrentThread[ContinuationInterceptor] as AndroidUiDispatcher
    val toRunTrampolined = dispatcher.javaClass
      .getDeclaredField("toRunTrampolined")
      .apply { isAccessible = true }
      .get(dispatcher) as ArrayDeque<*>
    toRunTrampolined.clear()
    // Upon reference leaks being fixed, verify we don't need to reset these values for
    // AndroidUiDispatcher to continue dispatching between tests.
    dispatcher.javaClass
      .getDeclaredField("scheduledTrampolineDispatch")
      .apply { isAccessible = true }
      .set(dispatcher, false)
    dispatcher.javaClass
      .getDeclaredField("scheduledFrameDispatch")
      .apply { isAccessible = true }
      .set(dispatcher, false)
  }

  private class PaparazziComposeOwner private constructor() :
    LifecycleOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
      get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry =
      savedStateRegistryController.savedStateRegistry

    companion object {
      fun register(view: View) {
        val owner = PaparazziComposeOwner()
        owner.savedStateRegistryController.performRestore(null)
        owner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
      }
    }
  }

  companion object {
    /** The choreographer doesn't like 0 as a frame time, so start an hour later. */
    internal val TIME_OFFSET_NANOS = TimeUnit.HOURS.toNanos(1L)

    internal lateinit var renderer: Renderer
    internal val isInitialized get() = ::renderer.isInitialized

    internal lateinit var sessionParamsBuilder: SessionParamsBuilder

    private val isVerifying: Boolean =
      System.getProperty("paparazzi.test.verify")?.toBoolean() == true

    private fun determineHandler(maxPercentDifference: Double): SnapshotHandler =
      if (isVerifying) {
        SnapshotVerifier(maxPercentDifference)
      } else {
        HtmlReportWriter()
      }
  }
}

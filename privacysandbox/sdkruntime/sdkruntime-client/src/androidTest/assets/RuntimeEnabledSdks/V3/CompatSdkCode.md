Test sdk that was built with V3 library.

DO NOT RECOMPILE WITH ANY CHANGES TO LIBRARY CLASSES.
Main purpose of that provider is to test that old core versions could be loaded by new client.

classes.dex built from:

1) androidx.privacysandbox.sdkruntime.core.Versions
@Keep
object Versions {

    const val API_VERSION = 3

    @JvmField
    var CLIENT_VERSION: Int? = null

    @JvmStatic
    fun handShake(clientVersion: Int): Int {
        CLIENT_VERSION = clientVersion
        return API_VERSION
    }
}

2) androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
abstract class SandboxedSdkProviderCompat {
    var context: Context? = null
        private set

    fun attachContext(context: Context) {
        check(this.context == null) { "Context already set" }
        this.context = context
    }

    @Throws(LoadSdkCompatException::class)
    abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat

    open fun beforeUnloadSdk() {}

    abstract fun getView(
            windowContext: Context,
            params: Bundle,
            width: Int,
            height: Int
    ): View
}

3) androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
class SandboxedSdkCompat private constructor(
   private val sdkImpl: SandboxedSdkImpl
) {

   constructor(sdkInterface: IBinder) : this(sdkInterface, sdkInfo = null)

   @Keep
   constructor(
      sdkInterface: IBinder,
      sdkInfo: SandboxedSdkInfo?
   ) : this(CompatImpl(sdkInterface, sdkInfo))

   fun getInterface() = sdkImpl.getInterface()

   fun getSdkInfo(): SandboxedSdkInfo? = sdkImpl.getSdkInfo()

   internal interface SandboxedSdkImpl {
      fun getInterface(): IBinder?
      fun getSdkInfo(): SandboxedSdkInfo?
   }

   private class CompatImpl(
      private val sdkInterface: IBinder,
      private val sdkInfo: SandboxedSdkInfo?
   ) : SandboxedSdkImpl {

      override fun getInterface(): IBinder {
         return sdkInterface
      }

      override fun getSdkInfo(): SandboxedSdkInfo? = sdkInfo
   }
}

4) androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
class LoadSdkCompatException : Exception {

    val loadSdkErrorCode: Int

    val extraInformation: Bundle

    @JvmOverloads
    constructor(
            loadSdkErrorCode: Int,
            message: String?,
            cause: Throwable?,
            extraInformation: Bundle = Bundle()
    ) : super(message, cause) {
        this.loadSdkErrorCode = loadSdkErrorCode
        this.extraInformation = extraInformation
    }

    constructor(
            cause: Throwable,
            extraInfo: Bundle
    ) : this(LOAD_SDK_SDK_DEFINED_ERROR, "", cause, extraInfo)

    companion object {
        const val LOAD_SDK_SDK_DEFINED_ERROR = 102
    }
}

5) androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
class SandboxedSdkInfo(
    val name: String,
    val version: Long
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SandboxedSdkInfo
        if (name != other.name) return false
        if (version != other.version) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }
}

6) androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
interface ActivityHolder : LifecycleOwner {
    fun getActivity(): Activity
    fun getOnBackPressedDispatcher(): OnBackPressedDispatcher
}

7) androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
interface SdkSandboxActivityHandlerCompat {
    fun onActivityCreated(activityHolder: ActivityHolder)
}

8) androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
class SdkSandboxControllerCompat internal constructor(
    private val controllerImpl: SandboxControllerImpl
) {
    fun getSandboxedSdks(): List<SandboxedSdkCompat> =
        controllerImpl.getSandboxedSdks()

    fun registerSdkSandboxActivityHandler(handlerCompat: SdkSandboxActivityHandlerCompat):
        IBinder = controllerImpl.registerSdkSandboxActivityHandler(handlerCompat)

    fun unregisterSdkSandboxActivityHandler(handlerCompat: SdkSandboxActivityHandlerCompat) =
        controllerImpl.unregisterSdkSandboxActivityHandler(handlerCompat)

    interface SandboxControllerImpl {
        fun getSandboxedSdks(): List<SandboxedSdkCompat>
        fun registerSdkSandboxActivityHandler(handlerCompat: SdkSandboxActivityHandlerCompat):
            IBinder
        fun unregisterSdkSandboxActivityHandler(
            handlerCompat: SdkSandboxActivityHandlerCompat
        )
    }

    companion object {
        private var localImpl: SandboxControllerImpl? = null
        @JvmStatic
        fun from(context: Context): SdkSandboxControllerCompat {
            val clientVersion = Versions.CLIENT_VERSION
            if (clientVersion != null) {
                val implFromClient = localImpl
                if (implFromClient != null) {
                    return SdkSandboxControllerCompat(LocalImpl(implFromClient, clientVersion))
                }
            }
            throw IllegalStateException("Should be loaded locally")
        }
        @JvmStatic
        @Keep
        fun injectLocalImpl(impl: SandboxControllerImpl) {
            check(localImpl == null) { "Local implementation already injected" }
            localImpl = impl
        }
    }
}

9) androidx.privacysandbox.sdkruntime.core.controller.impl.LocalImpl
internal class LocalImpl(
    private val implFromClient: SdkSandboxControllerCompat.SandboxControllerImpl,
    private val clientVersion: Int
) : SdkSandboxControllerCompat.SandboxControllerImpl {
    override fun getSandboxedSdks(): List<SandboxedSdkCompat> {
        return implFromClient.getSandboxedSdks()
    }

    override fun registerSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ): IBinder {
        if (clientVersion < 3) {
            throw UnsupportedOperationException(
                "Client library version doesn't support SdkActivities"
            )
        }
        return implFromClient.registerSdkSandboxActivityHandler(handlerCompat)
    }

    override fun unregisterSdkSandboxActivityHandler(
        handlerCompat: SdkSandboxActivityHandlerCompat
    ) {
        if (clientVersion < 3) {
            throw UnsupportedOperationException(
                "Client library version doesn't support SdkActivities"
            )
        }
        implFromClient.unregisterSdkSandboxActivityHandler(handlerCompat)
    }
}

10) androidx.privacysandbox.sdkruntime.test.v3.CompatProvider
class CompatProvider : SandboxedSdkProviderCompat() {

    @JvmField
    var onLoadSdkBinder: Binder? = null

    @JvmField
    var lastOnLoadSdkParams: Bundle? = null

    @JvmField
    var isBeforeUnloadSdkCalled = false

    @Throws(LoadSdkCompatException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        val result = SdkImpl(context!!)
        onLoadSdkBinder = result
        if (params.getBoolean("needFail", false)) {
            throw LoadSdkCompatException(RuntimeException(), params)
        }
        return SandboxedSdkCompat(result)
    }

    override fun beforeUnloadSdk() {
        isBeforeUnloadSdkCalled = true
    }

    override fun getView(
            windowContext: Context, params: Bundle, width: Int,
            height: Int
    ): View {
        return View(windowContext)
    }

    class SdkImpl(
        private val context: Context
    ) : Binder() {
        fun getSandboxedSdks(): List<SandboxedSdkCompat> =
            SdkSandboxControllerCompat.from(context).getSandboxedSdks()
        fun registerSdkSandboxActivityHandler(handler: SdkSandboxActivityHandlerCompat): IBinder =
            SdkSandboxControllerCompat.from(context).registerSdkSandboxActivityHandler(handler)
        fun unregisterSdkSandboxActivityHandler(handler: SdkSandboxActivityHandlerCompat) {
            SdkSandboxControllerCompat.from(context).unregisterSdkSandboxActivityHandler(handler)
        }
    }
}
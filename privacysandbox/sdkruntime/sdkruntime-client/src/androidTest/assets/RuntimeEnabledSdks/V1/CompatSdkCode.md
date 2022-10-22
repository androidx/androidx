Test sdk that was built with V1 library.

DO NOT RECOMPILE WITH ANY CHANGES TO LIBRARY CLASSES.
Main purpose of that provider is to test that old core versions could be loaded by new client.

classes.dex built from:

1) androidx.privacysandbox.sdkruntime.core.Versions
@Keep
object Versions {

    const val API_VERSION = 1

    @JvmField
    var CLIENT_VERSION = -1

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
sealed class SandboxedSdkCompat {

    abstract fun getInterface(): IBinder?

    private class CompatImpl(private val mInterface: IBinder) : SandboxedSdkCompat() {
        override fun getInterface(): IBinder? {
            return mInterface
        }
    }

    companion object {
        @JvmStatic
        fun create(binder: IBinder): SandboxedSdkCompat {
            return CompatImpl(binder)
        }
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

5) androidx.privacysandbox.sdkruntime.test.v1.CompatProvider
class CompatProvider : SandboxedSdkProviderCompat() {

    @JvmField
    val onLoadSdkBinder = Binder()

    @JvmField
    var lastOnLoadSdkParams: Bundle? = null

    @JvmField
    var isBeforeUnloadSdkCalled = false

    @Throws(LoadSdkCompatException::class)
    override fun onLoadSdk(params: Bundle): SandboxedSdkCompat {
        lastOnLoadSdkParams = params
        if (params.getBoolean("needFail", false)) {
            throw LoadSdkCompatException(RuntimeException(), params)
        }
        return SandboxedSdkCompat.create(onLoadSdkBinder)
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
}
package androidx.ui.engine.platform.io.platform

/**
 * Registry for platform view factories.
 *
 *
 * Plugins can register factories for specific view types.
 */
interface PlatformViewRegistry {

    /**
     * Registers a factory for a platform view.
     *
     * @param viewTypeId unique identifier for the platform view's type.
     * @param factory factory for creating platform views of the specified type.
     * @return true if succeeded, false if a factory is already registered for viewTypeId.
     */
    fun registerViewFactory(viewTypeId: String, factory: PlatformViewFactory): Boolean
}
package androidx.compose.ui.platform.a11y

import kotlinx.coroutines.*
import java.awt.Component
import java.awt.KeyboardFocusManager
import java.awt.event.FocusEvent
import java.beans.PropertyChangeEvent
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext
import org.jetbrains.skiko.MainUIDispatcher
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.hostOs
import org.jetbrains.skiko.initializeCAccessible

/**
 * A helper class for requesting accessibility focus on a given accessible.
 */
internal class AccessibleFocusHelper(
    private val component: Component,
    private val sceneAccessibleContext: AccessibleContext,
) {

    private var focusedAccessible: Accessible? = null

    val accessibleContext: AccessibleContext
        get() = focusedAccessible?.accessibleContext ?: sceneAccessibleContext

    private var resetFocusAccessibleJob: Job? = null

    @OptIn(DelicateCoroutinesApi::class)
    fun requestFocusOnAccessible(accessible: Accessible?) {
        focusedAccessible = accessible

        when (hostOs) {
            OS.Windows -> requestAccessBridgeFocusOnAccessible()
            OS.MacOS -> requestMacOSFocusOnAccessible(accessible)
            else -> {
                focusedAccessible = null
                return
            }
        }

        // Listener spawns asynchronous notification post procedure, reading current focus owner
        // and its accessibility context. This timeout is used to deal with concurrency
        // TODO Find more reliable procedure
        resetFocusAccessibleJob?.cancel()
        if (focusedAccessible != null) {
            resetFocusAccessibleJob = GlobalScope.launch(MainUIDispatcher) {
                delay(100)
                focusedAccessible = null
            }
        }
    }

    /**
     * When [focusedAccessible] is set, for the accessible hierarchy to be correct, its parent must
     * be reported as the scene's accessible context. This function returns it if [accessible] is
     * [focusedAccessible].
     */
    fun accessibleParentOverride(accessible: Accessible): Accessible? {
        return if (accessible == focusedAccessible) {
            sceneAccessibleContext.accessibleParent as Accessible
        } else {
            null
        }
    }

    private fun requestAccessBridgeFocusOnAccessible() {
        val focusEvent = FocusEvent(component, FocusEvent.FOCUS_GAINED)
        component.focusListeners.forEach { it.focusGained(focusEvent) }
    }

    private fun requestMacOSFocusOnAccessible(accessible: Accessible?) {
        val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
        val listeners = focusManager.getPropertyChangeListeners("focusOwner")
        val event = PropertyChangeEvent(focusManager, "focusOwner", null, accessible)
        listeners.forEach { it.propertyChange(event) }
    }

    fun dispose() {
        resetFocusAccessibleJob?.cancel()
    }
}

/**
 * This method should be called on custom [Accessible] creation (or its context if context is
 * created lazily).
 *
 * JDK's accessibility support (at least for macOS) builds mapping AccessibleContext -> Accessible.
 * Some [Accessible]s are built only when focus is settled and
 * since we have a hack [AccessibleFocusHelper.requestFocusOnAccessible], wrong mapping
 * can be built (ComponentAccessibleContext -> SkiaLayer instead of
 * ComponentAccessibleContext -> ComponentAccessible).
 *
 * This method forces JDK's accessibility support to cache mapping
 * ComponentAccessibleContext -> ComponentAccessible, if it is called on
 * ComponentAccessibleContext creation.
 *
 * Related to the [issue](https://youtrack.jetbrains.com/issue/COMPOSE-176).
 */
internal fun initializeAccessible(accessible: Accessible) {
    when (hostOs) {
        OS.MacOS -> {
            initializeCAccessible(accessible)
        }

        else -> {
            // TODO: do we need something for Windows?
        }
    }
}

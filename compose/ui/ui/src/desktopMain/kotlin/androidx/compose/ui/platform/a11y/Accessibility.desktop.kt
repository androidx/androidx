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
import androidx.compose.ui.scene.skia.SkiaLayerComponent
import kotlin.time.Duration.Companion.milliseconds

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
        initializeAccessible(accessible)

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
                delay(RESET_FOCUS_ACCESSIBLE_DELAY)
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

    companion object {
        val RESET_FOCUS_ACCESSIBLE_DELAY = 100.milliseconds
    }
}

/**
 * [sun.lwawt.macosx.CAccessible.getCAccessible] builds a mapping of [AccessibleContext] to
 * [sun.lwawt.macosx.CAccessible] instances (which wrap the corresponding [Accessible]).
 * If it is called with the Skia layer content [Accessible] (=[SkiaLayerComponent.contentRoot])
 * while the [AccessibleFocusHelper] hack is active ([AccessibleFocusHelper.focusedAccessible] is
 * not `null`), it builds an incorrect mapping, associating the focused [AccessibleContext] with
 * [SkiaLayerComponent.contentRoot].
 *
 * To work around this problem, [initializeAccessible] explicitly calls
 * [sun.lwawt.macosx.CAccessible.getCAccessible] on the focused [Accessible], forcing the correct
 * association to be made. Future calls then just retrieve the already stored value.
 *
 * See also [Error when following the instructions of
 * VoiceOver](https://youtrack.jetbrains.com/issue/CMP-176).
 */
private fun initializeAccessible(accessible: Accessible?) {
    if ((accessible != null) && (hostOs == OS.MacOS)) {
        initializeCAccessible(accessible)
    }
}

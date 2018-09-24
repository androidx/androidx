package androidx.ui.widgets.framework.key

import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.foundation.diagnostics.shortHash
import androidx.ui.runtimeType
import androidx.ui.widgets.framework.State
import androidx.ui.widgets.framework.StatefulWidget

/**
 * A global key with a debugging label.
 *
 * The debug label is useful for documentation and for debugging. The label
 * does not affect the key's identity.
 *
 * Ctor comment:
 * Creates a global key with a debugging label.
 *
 * The label does not affect the key's identity.
 */
// ignore: prefer_const_constructors_in_immutables , never use const for this class
class LabeledGlobalKey<T : State<StatefulWidget>>(
    private val _debugLabel: String
) : GlobalKey<T>() {

    override fun toString(): String {
        val label = if (_debugLabel != null) " $_debugLabel" else ""
        if (runtimeType().clazz == LabeledGlobalKey::class.java)
            return "[GlobalKey#${shortHash(this)}$label]"
        return "[${describeIdentity(this)}$label]"
    }
}
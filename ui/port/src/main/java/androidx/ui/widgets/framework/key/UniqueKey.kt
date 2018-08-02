package androidx.ui.widgets.framework.key

import androidx.ui.foundation.LocalKey
import androidx.ui.foundation.diagnostics.shortHash

/// A key that is only equal to itself.
///
/// Ctor comment:
/// Creates a key that is equal only to itself.
// ignore: prefer_const_constructors_in_immutables , never use const for this class
class UniqueKey : LocalKey() {

    override fun toString(): String = "[#${shortHash(this)}]";
}
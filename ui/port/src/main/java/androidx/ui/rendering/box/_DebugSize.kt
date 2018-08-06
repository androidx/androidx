package androidx.ui.rendering.box

import androidx.ui.engine.geometry.Size

// This class should only be used in debug builds.
class _DebugSize(val source: Size, val _owner: RenderBox, val _canBeUsedByParent: Boolean)
// TODO(Migration/andreykulikov): extends Size - but it is data class. probably we don't need this class at all anyway
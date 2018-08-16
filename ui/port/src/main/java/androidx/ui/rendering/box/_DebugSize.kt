package androidx.ui.rendering.box

import androidx.ui.engine.geometry.Size

/**
 * This class should only be used in debug builds.
 */
internal class _DebugSize(
    val source: Size,
    val _owner: RenderBox,
    val _canBeUsedByParent: Boolean
) : Size(source.width, source.height)
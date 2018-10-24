package androidx.ui.engine.text

import android.graphics.Typeface

// TODO(Migration/siyamed): shall this accept a string?
// TODO(Migration/siyamed): can we have access to context?
class FontFallback(val typeface: Typeface? = null) {
    override fun toString(): String {
        return "FontFallback(${if (typeface == null) "unspecified" else typeface.toString()})"
    }
}
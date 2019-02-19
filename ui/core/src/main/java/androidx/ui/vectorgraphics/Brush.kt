package androidx.ui.vectorgraphics

import androidx.ui.engine.geometry.Offset
import androidx.ui.painting.Color
import androidx.ui.painting.Gradient
import androidx.ui.painting.Paint
import androidx.ui.painting.TileMode
import androidx.ui.vectormath64.Matrix4

val EMPTY_BRUSH = object : Brush {
    override fun applyBrush(p: Paint) {
        // NO-OP
    }
}

interface Brush {
    fun applyBrush(p: Paint)
}

inline class FlatColor(private val value: Color) : Brush {
    override fun applyBrush(p: Paint) {
        p.color = value
    }
}

typealias ColorStop = Pair<Color, Float>

fun obtainBrush(brush: Any?): Any? {
    return when (brush) {
        is Int -> FlatColor(Color(brush))
        is Color -> FlatColor(brush)
        is Brush -> brush
        null -> null
        else -> throw IllegalArgumentException(brush.javaClass.simpleName +
                "Brush must be either a Color long, LinearGradient or RadialGradient")
    }
}

// TODO (njawad) replace with inline color class
class LinearGradient(
    vararg colorStops: ColorStop,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val colors: List<Color>
    private val stops: List<Float>

    init {
        colors = List(colorStops.size) { i -> colorStops[i].first }
        stops = List(colorStops.size) { i -> colorStops[i].second }
    }

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.linear(
            Offset(startX, startY),
            Offset(endX, endY),
            colors,
            stops,
            tileMode)
    }
}

class RadialGradient(
    vararg colorStops: ColorStop,
    private val centerX: Float,
    private val centerY: Float,
    private val radius: Float,
    private val tileMode: TileMode = TileMode.clamp
) : Brush {

    private val colors: List<Color>
    private val stops: List<Float>

    init {
        colors = List(colorStops.size) { it -> colorStops[it].first }
        stops = List(colorStops.size) { it -> colorStops[it].second }
    }

    override fun applyBrush(p: Paint) {
        p.shader = Gradient.radial(
            Offset(centerX, centerY),
            radius, colors, stops, tileMode, Matrix4(),
            null, 0.0f)
    }
}
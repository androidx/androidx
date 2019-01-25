package androidx.ui.vectorgraphics

class PathBuilder {

    private val nodes = mutableListOf<PathNode>()

    fun getNodes(): Array<PathNode> = nodes.toTypedArray()

    fun close(): PathBuilder =
        addNode(PathCommand.CLOSE)

    fun moveTo(x: Float, y: Float) =
        addNode(PathCommand.MOVE_TO, x, y)

    fun moveToRelative(x: Float, y: Float) =
        addNode(PathCommand.RELATIVE_MOVE_TO, x, y)

    fun lineTo(x: Float, y: Float) =
        addNode(PathCommand.LINE_TO, x, y)

    fun lineToRelative(x: Float, y: Float) =
        addNode(PathCommand.RELATIVE_LINE_TO, x, y)

    fun horizontalLineTo(x: Float) =
        addNode(PathCommand.HORIZONTAL_LINE_TO, x)

    fun horizontalLineToRelative(x: Float) =
        addNode(PathCommand.RELATIVE_HORIZONTAL_TO, x)

    fun verticalLineTo(y: Float) =
        addNode(PathCommand.VERTICAL_LINE_TO, y)

    fun verticalLineToRelative(y: Float) =
        addNode(PathCommand.RELATIVE_VERTICAL_TO, y)

    fun curveTo(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ) = addNode(PathCommand.CURVE_TO, x1, y1, x2, y2, x3, y3)

    fun curveToRelative(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        x3: Float,
        y3: Float
    ) = addNode(PathCommand.RELATIVE_CURVE_TO, x1, y1, x2, y2, x3, y3)

    fun reflectiveCurveTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.REFLECTIVE_CURVE_TO, x1, y1, x2, y2)

    fun reflectiveCurveToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.RELATIVE_REFLECTIVE_CURVE_TO, x1, y1, x2, y2)

    fun quadTo(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.QUAD_TO, x1, y1, x2, y2)

    fun quadToRelative(x1: Float, y1: Float, x2: Float, y2: Float) =
        addNode(PathCommand.RELATIVE_QUAD_TO, x1, y1, x2, y2)

    fun reflectiveQuadTo(x1: Float, y1: Float) =
        addNode(PathCommand.REFLECTIVE_QUAD_TO, x1, y1)

    fun reflectiveQuadToRelative(x1: Float, y1: Float) =
        addNode(PathCommand.RELATIVE_REFLECTIVE_QUAD_TO, x1, y1)

    fun arcTo(
        a: Float,
        b: Float,
        theta: Float,
        largeArcFlag: Float,
        sweepFlag: Float,
        x1: Float,
        y1: Float
    ) = addNode(PathCommand.ARC_TO, a, b, theta, largeArcFlag, sweepFlag, x1, y1)

    fun arcToRelative(
        a: Float,
        b: Float,
        theta: Float,
        largeArcFlag: Float,
        sweepFlag: Float,
        x1: Float,
        y1: Float
    ) = addNode(PathCommand.RELATIVE_ARC_TO, a, b, theta, largeArcFlag, sweepFlag, x1, y1)

    private fun addNode(cmd: PathCommand, vararg args: Float): PathBuilder {
        nodes.add(PathNode(cmd, args))
        return this
    }
}
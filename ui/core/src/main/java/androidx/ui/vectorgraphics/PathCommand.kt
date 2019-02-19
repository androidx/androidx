package androidx.ui.vectorgraphics

@Throws(UnknownPathCommandException::class)
fun pathCommandFromKey(key: Char): PathCommand {
    return when (key) {
        PathCommand.RELATIVE_CLOSE.key -> PathCommand.RELATIVE_CLOSE
        PathCommand.CLOSE.key -> PathCommand.CLOSE
        PathCommand.RELATIVE_MOVE_TO.key -> PathCommand.RELATIVE_MOVE_TO
        PathCommand.MOVE_TO.key -> PathCommand.MOVE_TO
        PathCommand.RELATIVE_LINE_TO.key -> PathCommand.RELATIVE_LINE_TO
        PathCommand.LINE_TO.key -> PathCommand.LINE_TO
        PathCommand.RELATIVE_HORIZONTAL_TO.key -> PathCommand.RELATIVE_HORIZONTAL_TO
        PathCommand.HORIZONTAL_LINE_TO.key -> PathCommand.HORIZONTAL_LINE_TO
        PathCommand.RELATIVE_VERTICAL_TO.key -> PathCommand.RELATIVE_VERTICAL_TO
        PathCommand.VERTICAL_LINE_TO.key -> PathCommand.VERTICAL_LINE_TO
        PathCommand.RELATIVE_CURVE_TO.key -> PathCommand.RELATIVE_CURVE_TO
        PathCommand.CURVE_TO.key -> PathCommand.CURVE_TO
        PathCommand.RELATIVE_REFLECTIVE_CURVE_TO.key -> PathCommand.RELATIVE_REFLECTIVE_CURVE_TO
        PathCommand.REFLECTIVE_CURVE_TO.key -> PathCommand.REFLECTIVE_CURVE_TO
        PathCommand.RELATIVE_QUAD_TO.key -> PathCommand.RELATIVE_QUAD_TO
        PathCommand.QUAD_TO.key -> PathCommand.QUAD_TO
        PathCommand.RELATIVE_REFLECTIVE_QUAD_TO.key -> PathCommand.RELATIVE_REFLECTIVE_QUAD_TO
        PathCommand.REFLECTIVE_QUAD_TO.key -> PathCommand.REFLECTIVE_QUAD_TO
        PathCommand.RELATIVE_ARC_TO.key -> PathCommand.RELATIVE_ARC_TO
        PathCommand.ARC_TO.key -> PathCommand.ARC_TO
        else -> throw UnknownPathCommandException(key)
    }
}

class UnknownPathCommandException(key: Char) : IllegalArgumentException("Unknown command for $key")

enum class PathCommand(val key: Char) {
    RELATIVE_CLOSE('z'),
    CLOSE('Z'),
    RELATIVE_MOVE_TO('m'),
    MOVE_TO('M'),
    RELATIVE_LINE_TO('l'),
    LINE_TO('L'),
    RELATIVE_HORIZONTAL_TO('h'),
    HORIZONTAL_LINE_TO('H'),
    RELATIVE_VERTICAL_TO('v'),
    VERTICAL_LINE_TO('V'),
    RELATIVE_CURVE_TO('c'),
    CURVE_TO('C'),
    RELATIVE_REFLECTIVE_CURVE_TO('s'),
    REFLECTIVE_CURVE_TO('S'),
    RELATIVE_QUAD_TO('q'),
    QUAD_TO('Q'),
    RELATIVE_REFLECTIVE_QUAD_TO('t'),
    REFLECTIVE_QUAD_TO('T'),
    RELATIVE_ARC_TO('a'),
    ARC_TO('A')
}
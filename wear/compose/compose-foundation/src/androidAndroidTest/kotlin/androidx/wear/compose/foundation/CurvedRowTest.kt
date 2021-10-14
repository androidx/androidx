import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AnchorType
import androidx.wear.compose.foundation.CurvedRow
import androidx.wear.compose.foundation.RadialAlignment
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.min

// When components are laid out, position is specified by integers, so we can't expect
// much precision.
internal const val FLOAT_TOLERANCE = 1f

class CurvedRowTest {
    @get:Rule
    val rule = createComposeRule()

    private fun anchor_and_clockwise_test(
        anchor: Float,
        anchorType: AnchorType,
        clockwise: Boolean,
        dimensionExtractor: (RadialDimensions) -> Float
    ) {
        var rowCoords: LayoutCoordinates? = null
        var coords: LayoutCoordinates? = null
        rule.setContent {
            CurvedRow(
                modifier = Modifier.size(200.dp)
                    .onGloballyPositioned { rowCoords = it },
                anchor = anchor,
                anchorType = anchorType,
                clockwise = clockwise
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .onGloballyPositioned { coords = it }
                )
            }
        }

        rule.runOnIdle {
            val dims = RadialDimensions(
                clockwise = clockwise,
                rowCoords!!,
                coords!!
            )

            // It's at the outer side of the CurvedRow,
            assertEquals(dims.rowRadius, dims.outerRadius, FLOAT_TOLERANCE)

            checkAngle(anchor, dimensionExtractor(dims))
        }
    }

    @Test
    fun correctly_uses_anchortype_start_clockwise() =
        anchor_and_clockwise_test(0f, AnchorType.Start, true) { it.startAngle }

    @Test
    fun correctly_uses_anchortype_center_clockwise() =
        anchor_and_clockwise_test(60f, AnchorType.Center, true) { it.middleAngle }

    @Test
    fun correctly_uses_anchortype_end_clockwise() =
        anchor_and_clockwise_test(120f, AnchorType.End, true) { it.endAngle }

    @Test
    fun correctly_uses_anchortype_start_anticlockwise() =
        anchor_and_clockwise_test(180f, AnchorType.Start, false) { it.endAngle }

    @Test
    fun correctly_uses_anchortype_center_anticlockwise() =
        anchor_and_clockwise_test(240f, AnchorType.Center, false) { it.middleAngle }

    @Test
    fun correctly_uses_anchortype_end_anticlockwise() =
        anchor_and_clockwise_test(300f, AnchorType.End, false) { it.startAngle }

    @Test
    fun lays_out_multiple_children_correctly() {
        var rowCoords: LayoutCoordinates? = null
        val coords = Array<LayoutCoordinates?>(3) { null }
        rule.setContent {
            CurvedRow(
                modifier = Modifier.onGloballyPositioned { rowCoords = it }
            ) {
                repeat(3) { ix ->
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .onGloballyPositioned { coords[ix] = it }
                    )
                }
            }
        }

        rule.runOnIdle {
            val dims = coords.map {
                RadialDimensions(
                    clockwise = true,
                    rowCoords!!,
                    it!!
                )
            }

            dims.forEach {
                // They are all at the outer side of the CurvedRow,
                // and have the same innerRadius and sweep
                assertEquals(it.rowRadius, it.outerRadius, FLOAT_TOLERANCE)
                assertEquals(dims[0].innerRadius, it.innerRadius, FLOAT_TOLERANCE)
                assertEquals(dims[0].sweep, it.sweep, FLOAT_TOLERANCE)
            }
            // There are one after another, the middle child is centered at 12 o clock
            checkAngle(dims[0].endAngle, dims[1].startAngle)
            checkAngle(dims[1].endAngle, dims[2].startAngle)
            checkAngle(270f, dims[1].middleAngle)
        }
    }

    private fun radial_alignment_test(
        radialAlignment: RadialAlignment,
        checker: (bigBoxDimensions: RadialDimensions, smallBoxDimensions: RadialDimensions) -> Unit
    ) {
        var rowCoords: LayoutCoordinates? = null
        var smallBoxCoords: LayoutCoordinates? = null
        var bigBoxCoords: LayoutCoordinates? = null
        // We have a big box and a small box with the specified alignment
        rule.setContent {
            CurvedRow(
                modifier = Modifier.onGloballyPositioned { rowCoords = it }
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .onGloballyPositioned { smallBoxCoords = it }
                        .radialAlignment(radialAlignment)
                )
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .onGloballyPositioned { bigBoxCoords = it }
                )
            }
        }

        rule.runOnIdle {
            val bigBoxDimensions = RadialDimensions(
                clockwise = true,
                rowCoords!!,
                bigBoxCoords!!
            )

            val smallBoxDimensions = RadialDimensions(
                clockwise = true,
                rowCoords!!,
                smallBoxCoords!!
            )

            // There are one after another
            checkAngle(smallBoxDimensions.endAngle, bigBoxDimensions.startAngle)

            checker(bigBoxDimensions, smallBoxDimensions)
        }
    }

    @Test
    fun radial_alignment_outer_works() =
        radial_alignment_test(RadialAlignment.Outer) { bigBoxDimensions, smallBoxDimensions ->
            assertEquals(
                bigBoxDimensions.outerRadius,
                smallBoxDimensions.outerRadius,
                FLOAT_TOLERANCE
            )
        }

    @Test
    fun radial_alignment_center_works() =
        radial_alignment_test(RadialAlignment.Center) { bigBoxDimensions, smallBoxDimensions ->
            assertEquals(
                bigBoxDimensions.centerRadius,
                smallBoxDimensions.centerRadius,
                FLOAT_TOLERANCE
            )
        }

    @Test
    fun radial_alignment_inner_works() =
        radial_alignment_test(RadialAlignment.Inner) { bigBoxDimensions, smallBoxDimensions ->
            assertEquals(
                bigBoxDimensions.innerRadius,
                smallBoxDimensions.innerRadius,
                FLOAT_TOLERANCE
            )
        }
}

fun checkAngle(expected: Float, actual: Float) {
    var d = expected - actual
    if (d < 0) d += 360f
    if (d > 180) d = 360f - d
    if (d > FLOAT_TOLERANCE) {
        fail("Angle is out of tolerance. Expected: $expected, actual: $actual")
    }
}

private fun Float.toRadians() = this * PI.toFloat() / 180f
private fun Float.toDegrees() = this * 180f / PI.toFloat()

private data class RadialPoint(val distance: Float, val angle: Float)

// Utility class to compute the dimensions of the annulus segment corresponding to a given component
// given that component's and the parent CurvedRow's LayoutCoordinates, and a boolean to indicate
// if the layout is clockwise or counterclockwise
private class RadialDimensions(
    clockwise: Boolean,
    rowCoords: LayoutCoordinates,
    coords: LayoutCoordinates
) {
    // Row dimmensions
    val rowCenter: Offset
    val rowRadius: Float
    // Component dimensions.
    val innerRadius: Float
    val outerRadius: Float
    val centerRadius
        get() = (innerRadius + outerRadius) / 2
    val sweep: Float
    val startAngle: Float
    val middleAngle: Float
    val endAngle: Float

    init {
        // Find the radius and center of the CurvedRow, all radial coordinates are relative to this
        // center
        rowRadius = min(rowCoords.size.width, rowCoords.size.height) / 2f
        rowCenter = rowCoords.localToRoot(
            Offset(
                rowCoords.size.width / 2f,
                rowCoords.size.height / 2f
            )
        )

        // Compute the radial coordinates (relative to the center of the CurvedRow) of the found
        // corners of the component's box and its center
        val width = coords.size.width.toFloat()
        val height = coords.size.height.toFloat()

        val topLeft = toRadialCoordinates(coords, 0f, 0f)
        val topRight = toRadialCoordinates(coords, width, 0f)
        val center = toRadialCoordinates(coords, width / 2f, height / 2f)
        val bottomLeft = toRadialCoordinates(coords, 0f, height)
        val bottomRight = toRadialCoordinates(coords, width, height)

        // Ensure the bottom corners are in the same circle
        assertEquals(bottomLeft.distance, bottomRight.distance, FLOAT_TOLERANCE)
        // Same with top corners
        assertEquals(topLeft.distance, topRight.distance, FLOAT_TOLERANCE)

        // Compute the four dimensions of the annulus sector
        // Note that startAngle is always before endAngle (even when going counterclockwise)
        if (clockwise) {
            innerRadius = bottomLeft.distance
            outerRadius = topLeft.distance
            startAngle = bottomLeft.angle.toDegrees()
            endAngle = bottomRight.angle.toDegrees()
        } else {
            // When components are laid out counterclockwise, they are rotated 180 degrees
            innerRadius = topLeft.distance
            outerRadius = bottomLeft.distance
            startAngle = topRight.angle.toDegrees()
            endAngle = topLeft.angle.toDegrees()
        }

        middleAngle = center.angle.toDegrees()
        sweep = if (endAngle > startAngle) {
            endAngle - startAngle
        } else {
            endAngle + 360f - startAngle
        }

        // All sweep angles are well between 0 and 90
        assert((FLOAT_TOLERANCE..90f - FLOAT_TOLERANCE).contains(sweep)) { "sweep = $sweep" }

        // The outerRadius is greater than the innerRadius
        assert(outerRadius > innerRadius + FLOAT_TOLERANCE) {
            "innerRadius = $innerRadius, outerRadius = $outerRadius"
        }
    }

    private fun toRadialCoordinates(coords: LayoutCoordinates, x: Float, y: Float): RadialPoint {
        val vector = coords.localToRoot(Offset(x, y)) - rowCenter
        return RadialPoint(vector.getDistance(), atan2(vector.y, vector.x))
    }
}
package androidx.ui.engine.geometry

// / A transform consisting of a translation, a rotation, and a uniform scale.
// /
// / Used by [Canvas.drawAtlas]. This is a more efficient way to represent these
// / simple transformations than a full matrix.
// Modeled after Skia's SkRSXform.
//
//
// Ctor comment:
// / Creates an RSTransform.
// /
// / An [RSTransform] expresses the combination of a translation, a rotation
// / around a particular point, and a scale factor.
// /
// / The first argument, `scos`, is the cosine of the rotation, multiplied by
// / the scale factor.
// /
// / The second argument, `ssin`, is the sine of the rotation, multiplied by
// / that same scale factor.
// /
// / The third argument is the x coordinate of the translation, minus the
// / `scos` argument multiplied by the x-coordinate of the rotation point, plus
// / the `ssin` argument multiplied by the y-coordinate of the rotation point.
// /
// / The fourth argument is the y coordinate of the translation, minus the `ssin`
// / argument multiplied by the x-coordinate of the rotation point, minus the
// / `scos` argument multiplied by the y-coordinate of the rotation point.
// /
// / The [new RSTransform.fromComponents] method may be a simpler way to
// / construct these values. However, if there is a way to factor out the
// / computations of the sine and cosine of the rotation so that they can be
// / reused over multiple calls to this constructor, it may be more efficient
// / to directly use this constructor instead.
data class RSTransform(
        // / The cosine of the rotation multiplied by the scale factor.
    val scos: Double,
        // / The sine of the rotation multiplied by that same scale factor.
    val ssin: Double,
        // / The x coordinate of the translation, minus [scos] multiplied by the
        // / x-coordinate of the rotation point, plus [ssin] multiplied by the
        // / y-coordinate of the rotation point.
    val tx: Double,
        // / The y coordinate of the translation, minus [ssin] multiplied by the
        // / x-coordinate of the rotation point, minus [scos] multiplied by the
        // / y-coordinate of the rotation point.
    val ty: Double
) {

   companion object {
       fun fromComponents(
           rotation: Double,
           scale: Double,
           anchorX: Double,
           anchorY: Double,
           translateX: Double,
           translateY: Double
       ): RSTransform {
           val scos = Math.cos(rotation) * scale
           val ssin = Math.sin(rotation) * scale
           val tx = translateX + -scos * anchorX + ssin * anchorY
           val ty = translateY + -ssin * anchorX - scos * anchorY
           return RSTransform(scos, ssin, tx, ty)
       }
   }
}

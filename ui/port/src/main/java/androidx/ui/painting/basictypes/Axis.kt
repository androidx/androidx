package androidx.ui.painting.basictypes

// / The two cardinal directions in two dimensions.
// /
// / The axis is always relative to the current coordinate space. This means, for
// / example, that a [horizontal] axis might actually be diagonally from top
// / right to bottom left, due to some local [Transform] applied to the scene.
// /
// / See also:
// /
// /  * [AxisDirection], which is a directional version of this enum (with values
// /    light left and right, rather than just horizontal).
// /  * [TextDirection], which disambiguates between left-to-right horizontal
// /    content and right-to-left horizontal content.
enum class Axis {
    // / Left and right.
    // /
    // / See also:
    // /
    // /  * [TextDirection], which disambiguates between left-to-right horizontal
    // /    content and right-to-left horizontal content.
    horizontal,

    // / Up and down.
    vertical
}

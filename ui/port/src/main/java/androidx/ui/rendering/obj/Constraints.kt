package androidx.ui.rendering.obj

import androidx.ui.foundation.assertions.InformationCollector

// / An abstract set of layout constraints.
// /
// / Concrete layout models (such as box) will create concrete subclasses to
// / communicate layout constraints between parents and children.
// /
// / ## Writing a Constraints subclass
// /
// / When creating a new [RenderObject] subclass with a new layout protocol, one
// / will usually need to create a new [Constraints] subclass to express the
// / input to the layout algorithms.
// /
// / A [Constraints] subclass should be immutable (all fields final). There are
// / several members to implement, in addition to whatever fields, constructors,
// / and helper methods one may find useful for a particular layout protocol:
// /
// / * The [isTight] getter, which should return true if the object represents a
// /   case where the [RenderObject] class has no choice for how to lay itself
// /   out. For example, [BoxConstraints] returns true for [isTight] when both
// /   the minimum and maximum widths and the minimum and maximum heights are
// /   equal.
// /
// / * The [isNormalized] getter, which should return true if the object
// /   represents its data in its canonical form. Sometimes, it is possible for
// /   fields to be redundant with each other, such that several different
// /   representations have the same implications. For example, a
// /   [BoxConstraints] instance with its minimum width greater than its maximum
// /   width is equivalent to one where the maximum width is set to that minimum
// /   width (`2<w<1` is equivalent to `2<w<2`, since minimum constraints have
// /   priority). This getter is used by the default implementation of
// /   [debugAssertIsValid].
// /
// / * The [debugAssertIsValid] method, which should assert if there's anything
// /   wrong with the constraints object. (We use this approach rather than
// /   asserting in constructors so that our constructors can be `const` and so
// /   that it is possible to create invalid constraints temporarily while
// /   building valid ones.) See the implementation of
// /   [BoxConstraints.debugAssertIsValid] for an example of the detailed checks
// /   that can be made.
// /
// / * The [==] operator and the [hashCode] getter, so that constraints can be
// /   compared for equality. If a render object is given constraints that are
// /   equal, then the rendering library will avoid laying the object out again
// /   if it is not dirty.
// /
// / * The [toString] method, which should describe the constraints so that they
// /   appear in a usefully readable form in the output of [debugDumpRenderTree].
// @immutable
abstract class Constraints {
    // / Abstract const constructor. This constructor enables subclasses to provide
    // / const constructors so that they can be used in const expressions.
    // TODO(Migration/xbhatnag): const constructor
    constructor()

    // / Whether there is exactly one size possible given these constraints
    abstract val isTight: Boolean

    // / Whether the constraint is expressed in a consistent manner.
    abstract val isNormalized: Boolean

    // / Asserts that the constraints are valid.
    // /
    // / This might involve checks more detailed than [isNormalized].
    // /
    // / For example, the [BoxConstraints] subclass verifies that the constraints
    // / are not [double.nan].
    // /
    // / If the `isAppliedConstraint` argument is true, then even stricter rules
    // / are enforced. This argument is set to true when checking constraints that
    // / are about to be applied to a [RenderObject] during layout, as opposed to
    // / constraints that may be further affected by other constraints. For
    // / example, the asserts for verifying the validity of
    // / [RenderConstrainedBox.additionalConstraints] do not set this argument, but
    // / the asserts for verifying the argument passed to the [RenderObject.layout]
    // / method do.
    // /
    // / The `informationCollector` argument takes an optional callback which is
    // / called when an exception is to be thrown. The collected information is
    // / then included in the message after the error line.
    // /
    // / Returns the same as [isNormalized] if asserts are disabled.
    fun debugAssertIsValid(
        isAppliedConstraint: Boolean = false,
        informationCollector: InformationCollector? = null
    ): Boolean {
        assert(isNormalized)
        return isNormalized
    }
}
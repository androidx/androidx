package androidx.ui.foundation.diagnostics

/**
 * The various priority levels used to filter which diagnostics are shown and
 * omitted.
 *
 * Trees of Flutter diagnostics can be very large so filtering the diagnostics
 * shown matters. Typically filtering to only show diagnostics with at least
 * level [debug] is appropriate.
 */
enum class DiagnosticLevel {
    /**
     * Diagnostics that should not be shown.
     *
     * If a user chooses to display [hidden] diagnostics, they should not expect
     * the diagnostics to be formatted consistently with other diagnostics and
     * they should expect them to sometimes be be misleading. For example,
     * [FlagProperty] and [ObjectFlagProperty] have uglier formatting when the
     * property `value` does does not match a value with a custom flag
     * description. An example of a misleading diagnostic is a diagnostic for
     * a property that has no effect because some other property of the object is
     * set in a way that causes the hidden property to have no effect.
     */
    hidden,

    /**
     * A diagnostic that is likely to be low value but where the diagnostic
     * display is just as high quality as a diagnostic with a higher level.
     *
     * Use this level for diagnostic properties that match their default value
     * and other cases where showing a diagnostic would not add much value such
     * as an [IterableProperty] where the value is empty.
     */
    fine,

    /**
     * Diagnostics that should only be shown when performing fine grained
     * debugging of an object.
     *
     * Unlike a [fine] diagnostic, these diagnostics provide important
     * information about the object that is likely to be needed to debug. Used by
     * properties that are important but where the property value is too verbose
     * (e.g. 300+ characters long) to show with a higher diagnostic level.
     */
    debug,

    /** Interesting diagnostics that should be typically shown. */
    info,

    /**
     * Very important diagnostics that indicate problematic property values.
     *
     * For example, use if you would write the property description
     * message in ALL CAPS.
     */
    warning,

    /**
     * Diagnostics that indicate errors or unexpected conditions.
     *
     * For example, use for property values where computing the value throws an
     * exception.
     */
    error,

    /**
     * Special level indicating that no diagnostics should be shown.
     *
     * Do not specify this level for diagnostics. This level is only used to
     * filter which diagnostics are shown.
     */
    off
}
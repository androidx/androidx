package androidx.glance

/**
 * Annotation for internal implementation details of the Glance API. This is intended for things we
 * need to expose to make them available in the same library group, but not intended for API
 * consumers and hence should not form part of our stable API.
 */
@RequiresOptIn(
    message = "This API is used for the implementation of androidx.glance, and should " +
        "not be used by API consumers."
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
public annotation class GlanceInternalApi

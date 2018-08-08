package androidx.ui.widgets.framework

// / This class is a work-around for the "is" operator not accepting a variable value as its right operand
class TypeMatcher protected constructor(private val clazz: Class<*>) {

    companion object {
        inline fun <reified T> create(): TypeMatcher {
            return TypeMatcher(T::class.java)
        }
    }

    // TODO(Migration/Filip): Hope this is correct
    // Orig was: object is T;
    fun check(obj: Any?) = if (obj == null) true else clazz.isAssignableFrom(obj::class.java)
}

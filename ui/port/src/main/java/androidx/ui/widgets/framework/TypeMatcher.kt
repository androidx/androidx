package androidx.ui.widgets.framework

// / This class is a work-around for the "is" operator not accepting a variable value as its right operand
class TypeMatcher<T> protected constructor(private val clazz: Class<*>) {

    companion object {
        inline fun <reified T> create(): TypeMatcher<T> {
            return TypeMatcher(T::class.java)
        }
    }

    // TODO(Migration/Filip): Hope this is correct
    fun check(obj: Any?) = if (obj == null) true else clazz.isAssignableFrom(obj::class.java) // Orig was: object is T;
}

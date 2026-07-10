package androidx.room3.integration.kotlintestapp.vo

import androidx.room3.ColumnTypeConverter
import androidx.room3.util.joinIntoString
import androidx.room3.util.splitToIntList

object StringToIntListConverters {
    @ColumnTypeConverter
    // Specifying that a static method should be generated. Otherwise, the compiler looks for the
    // constructor of the class, and a object has a private constructor.
    @JvmStatic
    fun stringToIntList(data: String?): List<Int>? =
        if (data == null) null else splitToIntList(data)

    @ColumnTypeConverter
    @JvmStatic
    fun intListToString(ints: List<Int>?): String? =
        if (ints == null) null else joinIntoString(ints)
}

package a.b

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.navigation.NavArgs
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import java.nio.file.AccessMode
import kotlin.Array
import kotlin.Boolean
import kotlin.Float
import kotlin.FloatArray
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmStatic

data class MainFragmentArgs(
    val main: String,
    val optional: Int = -1,
    val reference: Int = R.drawable.background,
    val referenceZeroDefaultValue: Int = 0,
    val floatArg: Float = 1F,
    val floatArrayArg: FloatArray,
    val objectArrayArg: Array<ActivityInfo>,
    val boolArg: Boolean = true,
    val optionalParcelable: ActivityInfo? = null,
    val enumArg: AccessMode = AccessMode.READ
) : NavArgs {
    @Suppress("CAST_NEVER_SUCCEEDS")
    fun toBundle(): Bundle {
        val result = Bundle()
        result.putString("main", this.main)
        result.putInt("optional", this.optional)
        result.putInt("reference", this.reference)
        result.putInt("referenceZeroDefaultValue", this.referenceZeroDefaultValue)
        result.putFloat("floatArg", this.floatArg)
        result.putFloatArray("floatArrayArg", this.floatArrayArg)
        result.putParcelableArray("objectArrayArg", this.objectArrayArg)
        result.putBoolean("boolArg", this.boolArg)
        if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            result.putParcelable("optionalParcelable", this.optionalParcelable as Parcelable?)
        } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
            result.putSerializable("optionalParcelable", this.optionalParcelable as Serializable?)
        }
        if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java)) {
            result.putParcelable("enumArg", this.enumArg as Parcelable)
        } else if (Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
            result.putSerializable("enumArg", this.enumArg as Serializable)
        }
        return result
    }

    companion object {
        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun fromBundle(bundle: Bundle): MainFragmentArgs {
            bundle.setClassLoader(MainFragmentArgs::class.java.classLoader)
            val __main : String?
            if (bundle.containsKey("main")) {
                __main = bundle.getString("main")
                if (__main == null) {
                    throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.")
                }
            } else {
                throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
            }
            val __optional : Int
            if (bundle.containsKey("optional")) {
                __optional = bundle.getInt("optional")
            } else {
                __optional = -1
            }
            val __reference : Int
            if (bundle.containsKey("reference")) {
                __reference = bundle.getInt("reference")
            } else {
                __reference = R.drawable.background
            }
            val __referenceZeroDefaultValue : Int
            if (bundle.containsKey("referenceZeroDefaultValue")) {
                __referenceZeroDefaultValue = bundle.getInt("referenceZeroDefaultValue")
            } else {
                __referenceZeroDefaultValue = 0
            }
            val __floatArg : Float
            if (bundle.containsKey("floatArg")) {
                __floatArg = bundle.getFloat("floatArg")
            } else {
                __floatArg = 1F
            }
            val __floatArrayArg : FloatArray?
            if (bundle.containsKey("floatArrayArg")) {
                __floatArrayArg = bundle.getFloatArray("floatArrayArg")
                if (__floatArrayArg == null) {
                    throw IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.")
                }
            } else {
                throw IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue")
            }
            val __objectArrayArg : Array<ActivityInfo>?
            if (bundle.containsKey("objectArrayArg")) {
                __objectArrayArg = bundle.getParcelableArray("objectArrayArg")?.map { it as
                        ActivityInfo }?.toTypedArray()
                if (__objectArrayArg == null) {
                    throw IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.")
                }
            } else {
                throw IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue")
            }
            val __boolArg : Boolean
            if (bundle.containsKey("boolArg")) {
                __boolArg = bundle.getBoolean("boolArg")
            } else {
                __boolArg = true
            }
            val __optionalParcelable : ActivityInfo?
            if (bundle.containsKey("optionalParcelable")) {
                if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java) ||
                        Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
                    __optionalParcelable = bundle.get("optionalParcelable") as ActivityInfo?
                } else {
                    throw UnsupportedOperationException(ActivityInfo::class.java.name +
                            " must implement Parcelable or Serializable or must be an Enum.")
                }
            } else {
                __optionalParcelable = null
            }
            val __enumArg : AccessMode?
            if (bundle.containsKey("enumArg")) {
                if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java) ||
                        Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
                    __enumArg = bundle.get("enumArg") as AccessMode?
                } else {
                    throw UnsupportedOperationException(AccessMode::class.java.name +
                            " must implement Parcelable or Serializable or must be an Enum.")
                }
                if (__enumArg == null) {
                    throw IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.")
                }
            } else {
                __enumArg = AccessMode.READ
            }
            return MainFragmentArgs(__main, __optional, __reference, __referenceZeroDefaultValue,
                    __floatArg, __floatArrayArg, __objectArrayArg, __boolArg, __optionalParcelable,
                    __enumArg)
        }
    }
}

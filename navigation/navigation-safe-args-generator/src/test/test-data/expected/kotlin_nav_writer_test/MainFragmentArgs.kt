package a.b

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.io.Serializable
import java.lang.IllegalArgumentException
import java.lang.UnsupportedOperationException
import java.nio.`file`.AccessMode
import kotlin.Array
import kotlin.Boolean
import kotlin.Float
import kotlin.FloatArray
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.jvm.JvmStatic

public data class MainFragmentArgs(
  public val main: String,
  public val floatArrayArg: FloatArray,
  public val objectArrayArg: Array<ActivityInfo>,
  public val optional: Int = -1,
  public val reference: Int = R.drawable.background,
  public val referenceZeroDefaultValue: Int = 0,
  public val floatArg: Float = 1F,
  public val boolArg: Boolean = true,
  public val optionalParcelable: ActivityInfo? = null,
  public val enumArg: AccessMode = AccessMode.READ
) : NavArgs {
  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toBundle(): Bundle {
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

  @Suppress("CAST_NEVER_SUCCEEDS")
  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("main", this.main)
    result.set("optional", this.optional)
    result.set("reference", this.reference)
    result.set("referenceZeroDefaultValue", this.referenceZeroDefaultValue)
    result.set("floatArg", this.floatArg)
    result.set("floatArrayArg", this.floatArrayArg)
    result.set("objectArrayArg", this.objectArrayArg)
    result.set("boolArg", this.boolArg)
    if (Parcelable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
      result.set("optionalParcelable", this.optionalParcelable as Parcelable?)
    } else if (Serializable::class.java.isAssignableFrom(ActivityInfo::class.java)) {
      result.set("optionalParcelable", this.optionalParcelable as Serializable?)
    }
    if (Parcelable::class.java.isAssignableFrom(AccessMode::class.java)) {
      result.set("enumArg", this.enumArg as Parcelable)
    } else if (Serializable::class.java.isAssignableFrom(AccessMode::class.java)) {
      result.set("enumArg", this.enumArg as Serializable)
    }
    return result
  }

  public companion object {
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    public fun fromBundle(bundle: Bundle): MainFragmentArgs {
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
        __objectArrayArg = bundle.getParcelableArray("objectArrayArg")?.map { it as ActivityInfo
            }?.toTypedArray()
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
      return MainFragmentArgs(__main, __floatArrayArg, __objectArrayArg, __optional, __reference,
          __referenceZeroDefaultValue, __floatArg, __boolArg, __optionalParcelable, __enumArg)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle): MainFragmentArgs {
      val __main : String?
      if (savedStateHandle.contains("main")) {
        __main = savedStateHandle["main"]
        if (__main == null) {
          throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
      }
      val __optional : Int?
      if (savedStateHandle.contains("optional")) {
        __optional = savedStateHandle["optional"]
        if (__optional == null) {
          throw IllegalArgumentException("Argument \"optional\" of type integer does not support null values")
        }
      } else {
        __optional = -1
      }
      val __reference : Int?
      if (savedStateHandle.contains("reference")) {
        __reference = savedStateHandle["reference"]
        if (__reference == null) {
          throw IllegalArgumentException("Argument \"reference\" of type reference does not support null values")
        }
      } else {
        __reference = R.drawable.background
      }
      val __referenceZeroDefaultValue : Int?
      if (savedStateHandle.contains("referenceZeroDefaultValue")) {
        __referenceZeroDefaultValue = savedStateHandle["referenceZeroDefaultValue"]
        if (__referenceZeroDefaultValue == null) {
          throw IllegalArgumentException("Argument \"referenceZeroDefaultValue\" of type reference does not support null values")
        }
      } else {
        __referenceZeroDefaultValue = 0
      }
      val __floatArg : Float?
      if (savedStateHandle.contains("floatArg")) {
        __floatArg = savedStateHandle["floatArg"]
        if (__floatArg == null) {
          throw IllegalArgumentException("Argument \"floatArg\" of type float does not support null values")
        }
      } else {
        __floatArg = 1F
      }
      val __floatArrayArg : FloatArray?
      if (savedStateHandle.contains("floatArrayArg")) {
        __floatArrayArg = savedStateHandle["floatArrayArg"]
        if (__floatArrayArg == null) {
          throw IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue")
      }
      val __objectArrayArg : Array<ActivityInfo>?
      if (savedStateHandle.contains("objectArrayArg")) {
        __objectArrayArg = savedStateHandle["objectArrayArg"]
        if (__objectArrayArg == null) {
          throw IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue")
      }
      val __boolArg : Boolean?
      if (savedStateHandle.contains("boolArg")) {
        __boolArg = savedStateHandle["boolArg"]
        if (__boolArg == null) {
          throw IllegalArgumentException("Argument \"boolArg\" of type boolean does not support null values")
        }
      } else {
        __boolArg = true
      }
      val __optionalParcelable : ActivityInfo?
      if (savedStateHandle.contains("optionalParcelable")) {
        __optionalParcelable = savedStateHandle["optionalParcelable"]
      } else {
        __optionalParcelable = null
      }
      val __enumArg : AccessMode?
      if (savedStateHandle.contains("enumArg")) {
        __enumArg = savedStateHandle["enumArg"]
        if (__enumArg == null) {
          throw IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value")
        }
      } else {
        __enumArg = AccessMode.READ
      }
      return MainFragmentArgs(__main, __floatArrayArg, __objectArrayArg, __optional, __reference,
          __referenceZeroDefaultValue, __floatArg, __boolArg, __optionalParcelable, __enumArg)
    }
  }
}

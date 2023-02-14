package a.b

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import java.lang.IllegalArgumentException
import kotlin.String
import kotlin.jvm.JvmStatic

public data class
    ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs(
  public val main: String,
) : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    result.putString("main", this.main)
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    result.set("main", this.main)
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle):
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs {
      bundle.setClassLoader(ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs::class.java.classLoader)
      val __main : String?
      if (bundle.containsKey("main")) {
        __main = bundle.getString("main")
        if (__main == null) {
          throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.")
        }
      } else {
        throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
      }
      return ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs(__main)
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle):
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs {
      val __main : String?
      if (savedStateHandle.contains("main")) {
        __main = savedStateHandle["main"]
        if (__main == null) {
          throw IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value")
        }
      } else {
        throw IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue")
      }
      return ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs(__main)
    }
  }
}

package a.b

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavArgs
import kotlin.jvm.JvmStatic

public data class
    ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs
    : NavArgs {
  public fun toBundle(): Bundle {
    val result = Bundle()
    return result
  }

  public fun toSavedStateHandle(): SavedStateHandle {
    val result = SavedStateHandle()
    return result
  }

  public companion object {
    @JvmStatic
    public fun fromBundle(bundle: Bundle):
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs {
      bundle.setClassLoader(ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs::class.java.classLoader)
      return ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs()
    }

    @JvmStatic
    public fun fromSavedStateHandle(savedStateHandle: SavedStateHandle):
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs
        =
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs()
  }
}

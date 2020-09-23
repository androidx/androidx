package a.b

import android.os.Bundle
import androidx.navigation.NavArgs
import kotlin.jvm.JvmStatic

data class
    ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs
    : NavArgs {
  fun toBundle(): Bundle {
    val result = Bundle()
    return result
  }

  companion object {
    @JvmStatic
    fun fromBundle(bundle: Bundle):
        ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs {
      bundle.setClassLoader(ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs::class.java.classLoader)
      return ReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyReallyLongNameFragmentArgs()
    }
  }
}

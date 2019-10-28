package foo.flavor

import androidx.navigation.NavDirections
import foo.InnerSettingsDirections
import kotlin.Int
import kotlin.String

class InnerSettingsFragmentDirections private constructor() {
  companion object {
    fun exit(exitReason: Int): NavDirections = InnerSettingsDirections.exit(exitReason)

    fun main(enterReason: String = "DEFAULT"): NavDirections =
        InnerSettingsDirections.main(enterReason)
  }
}

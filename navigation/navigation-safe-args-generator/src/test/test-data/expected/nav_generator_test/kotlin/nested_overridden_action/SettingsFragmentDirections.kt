package foo.flavor

import androidx.navigation.NavDirections
import foo.SettingsDirections
import kotlin.String

class SettingsFragmentDirections private constructor() {
  companion object {
    fun main(enterReason: String = "DEFAULT"): NavDirections = SettingsDirections.main(enterReason)

    fun exit(exitReason: String = "DEFAULT"): NavDirections = SettingsDirections.exit(exitReason)
  }
}

package foo.flavor

import androidx.navigation.NavDirections
import foo.SettingsDirections
import kotlin.String

public class SettingsFragmentDirections private constructor() {
  public companion object {
    public fun main(enterReason: String = "DEFAULT"): NavDirections =
        SettingsDirections.main(enterReason)

    public fun exit(exitReason: String = "DEFAULT"): NavDirections =
        SettingsDirections.exit(exitReason)
  }
}

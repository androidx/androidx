package foo.flavor

import androidx.navigation.NavDirections
import foo.InnerSettingsDirections
import kotlin.Int
import kotlin.String

public class InnerSettingsFragmentDirections private constructor() {
  public companion object {
    public fun exit(exitReason: Int): NavDirections = InnerSettingsDirections.exit(exitReason)

    public fun main(enterReason: String = "DEFAULT"): NavDirections =
        InnerSettingsDirections.main(enterReason)
  }
}

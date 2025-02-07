package foo.flavor

import androidx.`annotation`.CheckResult
import androidx.navigation.NavDirections
import foo.SettingsDirections
import kotlin.String

public class SettingsFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun main(enterReason: String = "DEFAULT"): NavDirections =
        SettingsDirections.main(enterReason)

    @CheckResult
    public fun exit(exitReason: String = "DEFAULT"): NavDirections =
        SettingsDirections.exit(exitReason)
  }
}

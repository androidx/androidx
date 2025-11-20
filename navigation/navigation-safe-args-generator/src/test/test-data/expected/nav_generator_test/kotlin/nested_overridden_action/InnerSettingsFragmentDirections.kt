package foo.flavor

import androidx.`annotation`.CheckResult
import androidx.navigation.NavDirections
import foo.InnerSettingsDirections
import kotlin.Int
import kotlin.String

public class InnerSettingsFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun exit(exitReason: Int): NavDirections = InnerSettingsDirections.exit(exitReason)

    @CheckResult
    public fun main(enterReason: String = "DEFAULT"): NavDirections = InnerSettingsDirections.main(enterReason)
  }
}

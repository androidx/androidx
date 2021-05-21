package foo.flavor

import android.os.Bundle
import androidx.navigation.NavDirections
import foo.R
import kotlin.Int
import kotlin.String

public class SettingsFragmentDirections private constructor() {
  private data class Exit(
    public val exitReason: String = "DIFFERENT"
  ) : NavDirections {
    public override val actionId: Int = R.id.exit

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("exitReason", this.exitReason)
        return result
      }
  }

  public companion object {
    public fun exit(exitReason: String = "DIFFERENT"): NavDirections = Exit(exitReason)
  }
}

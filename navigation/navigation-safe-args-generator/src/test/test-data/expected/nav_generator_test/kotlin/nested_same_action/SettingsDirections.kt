package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class SettingsDirections private constructor() {
  private data class Exit(
    public val exitReason: String = "DEFAULT"
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
    public fun exit(exitReason: String = "DEFAULT"): NavDirections = Exit(exitReason)
  }
}

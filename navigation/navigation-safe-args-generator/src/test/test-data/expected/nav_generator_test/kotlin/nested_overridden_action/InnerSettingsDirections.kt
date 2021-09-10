package foo

import android.os.Bundle
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class InnerSettingsDirections private constructor() {
  private data class Exit(
    public val exitReason: Int
  ) : NavDirections {
    public override val actionId: Int = R.id.exit

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putInt("exitReason", this.exitReason)
        return result
      }
  }

  public companion object {
    public fun exit(exitReason: Int): NavDirections = Exit(exitReason)

    public fun main(enterReason: String = "DEFAULT"): NavDirections =
        SettingsDirections.main(enterReason)
  }
}

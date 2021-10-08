package a.b

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import kotlin.Int
import kotlin.String

public class MainFragmentDirections private constructor() {
  private data class Next(
    public val main: String,
    public val optional: String = "bla"
  ) : NavDirections {
    public override val actionId: Int = R.id.next

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("main", this.main)
        result.putString("optional", this.optional)
        return result
      }
  }

  public companion object {
    public fun previous(): NavDirections = ActionOnlyNavDirections(R.id.previous)

    public fun next(main: String, optional: String = "bla"): NavDirections = Next(main, optional)
  }
}

package a.b

import android.os.Bundle
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import kotlin.Float
import kotlin.Int
import kotlin.String

public class MainFragmentDefaultParamDirections private constructor() {
  private data class Next(
    public val main: String,
    public val optional: String = "bla",
    public val optionalFloat: Float = 0.1F,
    public val optionalInt: Int = 1
  ) : NavDirections {
    public override val actionId: Int = R.id.next

    public override val arguments: Bundle
      get() {
        val result = Bundle()
        result.putString("optional", this.optional)
        result.putFloat("optionalFloat", this.optionalFloat)
        result.putString("main", this.main)
        result.putInt("optionalInt", this.optionalInt)
        return result
      }
  }

  public companion object {
    public fun previous(): NavDirections = ActionOnlyNavDirections(R.id.previous)

    public fun next(
      main: String,
      optional: String = "bla",
      optionalFloat: Float = 0.1F,
      optionalInt: Int = 1
    ): NavDirections = Next(main, optional, optionalFloat, optionalInt)
  }
}

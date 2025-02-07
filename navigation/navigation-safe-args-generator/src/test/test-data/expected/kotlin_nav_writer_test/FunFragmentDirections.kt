package a.b

import `fun`.`is`.`in`.R
import androidx.`annotation`.CheckResult
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class FunFragmentDirections private constructor() {
  public companion object {
    @CheckResult
    public fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
  }
}

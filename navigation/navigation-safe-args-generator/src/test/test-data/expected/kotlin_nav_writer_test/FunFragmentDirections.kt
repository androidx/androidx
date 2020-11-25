package a.b

import `fun`.`is`.`in`.R
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

public class FunFragmentDirections private constructor() {
  public companion object {
    public fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
  }
}

package a.b

import `fun`.`is`.`in`.R
import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections

class FunFragmentDirections private constructor() {
  companion object {
    fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
  }
}

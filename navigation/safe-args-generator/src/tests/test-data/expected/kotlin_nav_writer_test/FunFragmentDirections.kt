package a.b

import androidx.navigation.ActionOnlyNavDirections
import androidx.navigation.NavDirections
import `fun`.`is`.`in`.R

class FunFragmentDirections private constructor() {
    companion object {
        fun next(): NavDirections = ActionOnlyNavDirections(R.id.next)
    }
}

package foo.flavor.account

import androidx.navigation.NavDirections
import foo.LoginDirections

class RegisterFragmentDirections private constructor() {
  companion object {
    fun actionDone(): NavDirections = LoginDirections.actionDone()
  }
}

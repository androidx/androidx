If after restoring the saved state, the build passed, it might mean that there is additional state being saved in another place that we don't know about
This might mean that there is additional state being saved somewhere else that this script does not know about
This might mean that the success or failure status of the build is dependent on timestamps.
This might mean that the build is nondeterministic.
Unfortunately, this script does not know how to diagnose this further.
You could:
  A) Ask a team member if they know where the state may be stored
  B) Ask a team member if they recognize the build error
  C) Look for files named OWNERS to give you ideas of who to ask for help
  D) Run `git log` to give you ideas of who to ask for help

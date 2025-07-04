# SceneCore FSM HSM Transition Sample App README

This sample app demonstrates how an application transition from FSM -> HSM
and HSM -> FSM.

## To test 'Launch with Inherited Environment' feature

Required following workaround steps:

1.  `adb root`

1.  `adb remount`

1.  `adb reboot`

1.  `adb root`

1.  `adb remount`

1.  `adb push <apk file> /system/priv-app`

1.  `adb push privapp-permissions-samples.xml /etc/permissions`

1.  `adb reboot`
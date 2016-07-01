. $ANDROID_BUILD_TOP/build/envsetup.sh && \
mmm -j20 . && \
adb install -r $OUT/data/app/SupportAnimatedVectorDrawable/SupportAnimatedVectorDrawable.apk && \
adb shell am start -n com.example.android.support.vectordrawable/com.example.android.support.vectordrawable.app.AnimatedButtonBackground



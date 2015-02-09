. ../../../../build/envsetup.sh
mmm -j20 . && mmm -j20 ./tests/ && \
adb install -r $OUT/data/app/AndroidVectorDrawableTests/AndroidVectorDrawableTests.apk && \
adb shell am start -n android.support.v7.vectordrawable/android.support.v7.vectordrawable.TestActivity


jarsigner -keystore ~/.android/debug.keystore -storepass android $1 androiddebugkey \
&& adb install -r -t $1 && rm -rf out && java -jar  ../KVM/baksmali-2.2.2.jar d $1


#!/bin/bash -ex

# This script updates the prebuilt test_framework-sdkextension.jar, which is
# required when the "new APIs" added change.

function gettop() {
    local p=$(pwd)
    while [[ ! -e "${p}/build/envsetup.sh" ]]; do
        p="${p}/.."
    done
    echo $(readlink -f $p)
}

function is_aosp() {
    grep -q 'https://android-review.googlesource.com' $(gettop)/.repo/manifests/default.xml
}

if [[ -z "$OUT" ]]; then
    echo "lunch first"
    exit 1
fi

dir=$(dirname $(readlink -f $BASH_SOURCE))
bps="${dir}/../framework/Android.bp"
# AOSP does not use combined stubs, so needs special treatment.
if is_aosp; then
    bps="$bps $(gettop)/frameworks/base/Android.bp"
fi

for bp in $bps; do
    if ! test -e $bp; then
        echo $bp does not exist
        exit 1
    elif test -e "${bp}.bak"; then
        echo "skipping ${bp} modification because ${bp}.bak exists"
        continue
    fi
    cp $bp "${bp}.bak"
    sed -i -e 's|":framework-sdkextensions-sources"|":framework-sdkextensions-sources",":test_framework-sdkextensions-sources"|' $bp
done

$(gettop)/build/soong/soong_ui.bash --make-mode framework-sdkextensions

for bp in $bps; do mv "${bp}.bak" $bp ; touch $bp; done
cp "${OUT}/apex/com.android.sdkext/javalib/framework-sdkextensions.jar" "${dir}/test_framework-sdkextensions.jar"

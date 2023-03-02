#!/bin/bash
if ! [ -e build/soong ]; then
  echo "This script must be run from the top of the tree"
  exit 1
fi

sdk="$1"
if [[ -z "$sdk" ]]; then
  echo "usage: $0 <new-sdk-int> [modules] [bug-id]"
  exit 1
fi
shift

if [[ -n $1 ]] && ! [[ $1 =~ [0-9]+ ]]; then
  modules_arg="--modules $1"
  shift
fi

bug_text=$(test -n "$1" && echo "\nBug: $1")

SDKEXT="packages/modules/SdkExtensions/"

TARGET_PRODUCT=aosp_arm64 build/soong/soong_ui.bash --make-mode --soong-only gen_sdk
out/soong/host/linux-x86/bin/gen_sdk \
    --database ${SDKEXT}/gen_sdk/extensions_db.textpb \
    --action new_sdk \
    --sdk "$sdk" \
    $modules_arg
sed -E -i -e "/public static final int CURRENT_TRAIN_VERSION = /{s/\S+;/${sdk};/}" \
    ${SDKEXT}/java/com/android/os/ext/testing/CurrentVersion.java
repo start bump-ext ${SDKEXT}

message="Bump SDK Extension version to ${sdk}

Generated with:
$ $0 $@

Database update generated with:
$ gen_sdk --action new_sdk --sdk $sdk
"
message+=$(test -n "$2" && echo -e "\nBug: $2")
message+=$(echo -e "\nTest: presubmit")

git -C ${SDKEXT} commit -a -m "$message"
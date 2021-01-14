/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "derive_sdk"

#include "derive_sdk.h"

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-modules-utils/sdk_level.h>
#include <dirent.h>
#include <sys/stat.h>

#include <algorithm>
#include <iostream>
#include <vector>

#include "packages/modules/SdkExtensions/proto/sdk.pb.h"

namespace android {
namespace derivesdk {

bool SetSdkLevels(const std::string& mountpath) {
  std::unique_ptr<DIR, decltype(&closedir)> apex(opendir(mountpath.c_str()),
                                                 closedir);
  if (!apex) {
    LOG(ERROR) << "Could not read " + mountpath;
    return false;
  }
  struct dirent* de;
  std::vector<std::string> paths;
  while ((de = readdir(apex.get()))) {
    std::string name = de->d_name;
    if (name[0] == '.' || name.find('@') != std::string::npos) {
      // Skip <name>@<ver> dirs, as they are bind-mounted to <name>
      continue;
    }
    std::string path = mountpath + "/" + name + "/etc/sdkinfo.binarypb";
    struct stat statbuf;
    if (stat(path.c_str(), &statbuf) == 0) {
      paths.push_back(path);
    }
  }

  std::vector<int> versions;
  for (const auto& path : paths) {
    std::string contents;
    if (!android::base::ReadFileToString(path, &contents, true)) {
      LOG(ERROR) << "failed to read " << path;
      continue;
    }
    SdkVersion sdk_version;
    if (!sdk_version.ParseFromString(contents)) {
      LOG(ERROR) << "failed to parse " << path;
      continue;
    }
    LOG(INFO) << "Read version " << sdk_version.version() << " from " << path;
    versions.push_back(sdk_version.version());
  }
  auto itr = std::min_element(versions.begin(), versions.end());
  std::string prop_value = itr == versions.end() ? "0" : std::to_string(*itr);

  if (!android::base::SetProperty("build.version.extensions.r", prop_value)) {
    LOG(ERROR) << "failed to set r sdk_info prop";
    return false;
  }
  if (android::modules::sdklevel::IsAtLeastS()) {
    if (!android::base::SetProperty("build.version.extensions.s", prop_value)) {
      LOG(ERROR) << "failed to set s sdk_info prop";
      return false;
    }
  }

  LOG(INFO) << "Extension version is " << prop_value;
  return true;
}

}  // namespace derivesdk
}  // namespace android

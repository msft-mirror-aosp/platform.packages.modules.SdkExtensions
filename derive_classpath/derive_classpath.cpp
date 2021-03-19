/*
 * Copyright (C) 2021 The Android Open Source Project
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

#include "derive_classpath.h"
#include <android-base/file.h>
#include <android-base/logging.h>

namespace android {
namespace derive_classpath {

static const std::string kBaseClasspathExportsFilepath = "/system/etc/classpath";
static const std::string kGeneratedClasspathExportsFilepath = "/data/system/environ/classpath";

bool GenerateClasspathExports() {
  std::string contents;
  if (!android::base::ReadFileToString(kBaseClasspathExportsFilepath, &contents)) {
    PLOG(ERROR) << "Failed to read " << kBaseClasspathExportsFilepath;
    return false;
  }

  if (!android::base::WriteStringToFile(contents, kGeneratedClasspathExportsFilepath)) {
    PLOG(ERROR) << "Failed to write " << kGeneratedClasspathExportsFilepath;
  }

  return true;
}

}  // namespace derive_classpath
}  // namespace android

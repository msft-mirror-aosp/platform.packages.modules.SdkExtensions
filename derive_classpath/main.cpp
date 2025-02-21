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

#include <cstdlib>
#include <string_view>

#include <android-base/logging.h>
#include <android-base/parseint.h>
#include <android-base/strings.h>

#include "derive_classpath.h"

bool ArgumentMatches(std::string_view argument, std::string_view prefix, std::string_view* value) {
  if (android::base::StartsWith(argument, prefix)) {
    *value = argument.substr(prefix.size());
    return true;
  }
  return false;
}

// Command line flags need to be considered as a de facto API since there may be callers outside
// of the SdkExtensions APEX, which needs to run on older Android versions. For example, otapreopt
// currently executes derive_classpath with a single output file. When changing the flags, make sure
// it won't break on older Android.
bool ParseArgs(android::derive_classpath::Args& args, int argc, char** argv) {
  // Parse flags
  std::vector<std::string_view> positional_args;
  for (int i = 1; i < argc; ++i) {
    const std::string_view arg = argv[i];
    std::string_view value;
    if (ArgumentMatches(arg, "--bootclasspath-fragment=", &value)) {
      if (!args.system_bootclasspath_fragment.empty()) {
        LOG(ERROR) << "Duplicated flag --bootclasspath-fragment is specified";
        return false;
      }
      args.system_bootclasspath_fragment = value;
    } else if (ArgumentMatches(arg, "--systemserverclasspath-fragment=", &value)) {
      if (!args.system_systemserverclasspath_fragment.empty()) {
        LOG(ERROR) << "Duplicated flag --systemserverclasspath-fragment is specified";
        return false;
      }
      args.system_systemserverclasspath_fragment = value;
    } else if (ArgumentMatches(arg, "--scan-dirs=", &value)) {
      if (!args.scan_dirs.empty()) {
        LOG(ERROR) << "Duplicated flag --scan-dirs is specified";
        return false;
      }
      args.scan_dirs = android::base::Split(std::string(value), ",");
    } else if (ArgumentMatches(arg, "--glob-pattern-prefix=", &value)) {
      if (!args.glob_pattern_prefix.empty()) {
        LOG(ERROR) << "Duplicated flag --glob-pattern-prefix is specified";
        return false;
      }
      args.glob_pattern_prefix = value;
    } else if (ArgumentMatches(arg, "--override-device-sdk-version=", &value)) {
      if (args.override_device_sdk_version != 0) {
        LOG(ERROR) << "Duplicated flag --override-device-sdk-version is specified";
        return false;
      }
      if (!android::base::ParseInt(std::string(value), &args.override_device_sdk_version, /*min=*/1,
                                   /*max=*/INT_MAX)) {
        PLOG(ERROR) << "Invalid value for --override-device-sdk-version \"" << value << "\"";
        return false;
      }
    } else if (ArgumentMatches(arg, "--override-device-codename=", &value)) {
      if (!args.override_device_codename.empty()) {
        LOG(ERROR) << "Duplicated flag --override-device-codename is specified";
        return false;
      }
      args.override_device_codename = value;
    } else if (ArgumentMatches(arg, "--override-device-known-codenames=", &value)) {
      if (!args.override_device_known_codenames.empty()) {
        LOG(ERROR) << "Duplicated flag --override-device-known-codenames is specified";
        return false;
      }
      std::vector<std::string> known_codenames = android::base::Split(std::string(value), ",");
      std::move(known_codenames.begin(), known_codenames.end(),
                std::inserter(args.override_device_known_codenames,
                              args.override_device_known_codenames.end()));
    } else {
      positional_args.emplace_back(arg);
    }
  }

  // Validate flag combinations
  if (!args.scan_dirs.empty() && (!args.system_bootclasspath_fragment.empty() ||
                                  !args.system_systemserverclasspath_fragment.empty())) {
    LOG(ERROR) << "--scan-dirs should not be accompanied by --bootclasspath-fragment or "
                  "--systemserverclasspath-fragment";
    return false;
  }

  if (!args.glob_pattern_prefix.empty() &&
      (!args.scan_dirs.empty() || !args.system_bootclasspath_fragment.empty() ||
       !args.system_systemserverclasspath_fragment.empty())) {
    LOG(ERROR) << "--glob-pattern-prefix should not be accompanied by --scan-dirs, "
                  "--bootclasspath-fragment or --systemserverclasspath-fragment";
    return false;
  }

  if (args.override_device_sdk_version != 0 && args.override_device_codename.empty()) {
    LOG(ERROR)
        << "--override-device-sdk-version should be accompanied by --override-device-codename";
    return false;
  }

  if (!args.override_device_codename.empty() && args.override_device_codename != "REL" &&
      args.override_device_known_codenames.empty()) {
    LOG(ERROR) << "--override-device-codename should be accompanied by "
                  "--override-device-known-codenames, unless it is set to \"REL\"";
    return false;
  }

  if (args.override_device_sdk_version == 0 &&
      (!args.override_device_codename.empty() || !args.override_device_known_codenames.empty())) {
    LOG(ERROR) << "--override-device-codename and --override-device-known-codenames should not "
                  "be specified without --override-device-sdk-version";
    return false;
  }

#ifndef SDKEXT_ANDROID
  if (args.glob_pattern_prefix.empty() && args.scan_dirs.empty()) {
    LOG(ERROR) << "Either --glob-pattern-prefix or --scan-dirs must be specified on host";
    return false;
  }

  if (args.override_device_sdk_version == 0) {
    LOG(ERROR)
        << "--override-device-sdk-version and --override-device-codename must be specified on host";
    return false;
  }
#endif

  // Handle positional args
  if (positional_args.size() == 0) {
#ifndef SDKEXT_ANDROID
    LOG(ERROR) << "Output path must be specified on host";
    return false;
#endif
    args.output_path = android::derive_classpath::kGeneratedClasspathExportsFilepath;
  } else if (positional_args.size() == 1) {
    args.output_path = positional_args[0];
  } else {
    LOG(ERROR) << "Unrecognized positional arguments: "
               << android::base::Join(positional_args, ' ');
    return false;
  }

  return true;
}

int main(int argc, char** argv) {
  android::derive_classpath::Args args;
  if (!ParseArgs(args, argc, argv)) {
    return EXIT_FAILURE;
  }
  if (!android::derive_classpath::GenerateClasspathExports(args)) {
    return EXIT_FAILURE;
  }
  return EXIT_SUCCESS;
}

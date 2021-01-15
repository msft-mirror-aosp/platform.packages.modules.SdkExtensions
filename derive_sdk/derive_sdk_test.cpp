/*
 * Copyright (C) 2020 The Android Open Source Project
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

#define LOG_TAG "derive_sdk_test"

#include "derive_sdk.h"

#include <android-base/file.h>
#include <android-base/logging.h>
#include <android-base/properties.h>
#include <android-modules-utils/sdk_level.h>
#include <gtest/gtest.h>
#include <sys/stat.h>

#include <cstdlib>

#include "packages/modules/SdkExtensions/proto/sdk.pb.h"

class DeriveSdkTest : public ::testing::Test {
 protected:
  void TearDown() override { android::derivesdk::SetSdkLevels("/apex"); }

  std::string dir() { return std::string(dir_.path); }

  void MakeSdkVersion(std::string apex, int version) {
    SdkVersion sdk_version;
    sdk_version.set_version(version);
    std::string buf;
    ASSERT_TRUE(sdk_version.SerializeToString(&buf));
    std::string path = dir() + "/" + apex;
    ASSERT_EQ(0, mkdir(path.c_str(), 0755));
    path += "/etc";
    ASSERT_EQ(0, mkdir(path.c_str(), 0755));
    path += "/sdkinfo.binarypb";
    ASSERT_TRUE(android::base::WriteStringToFile(buf, path, true));
  }

  void EXPECT_R(int n) {
    int R = android::base::GetIntProperty("build.version.extensions.r", -1);
    EXPECT_EQ(R, n);
  }

  void EXPECT_S(int n) {
    int S = android::base::GetIntProperty("build.version.extensions.s", -1);
    // Only expect the S extension level to be set on S+ devices.
    EXPECT_EQ(S, android::modules::sdklevel::IsAtLeastS() ? n : -1);
  }

  TemporaryDir dir_;
};

TEST_F(DeriveSdkTest, CurrentSystemImageValue) {
  EXPECT_R(0);
  EXPECT_S(0);
}

TEST_F(DeriveSdkTest, OneApex) {
  MakeSdkVersion("a", 3);

  android::derivesdk::SetSdkLevels(dir());

  EXPECT_R(3);
  EXPECT_S(3);
}

TEST_F(DeriveSdkTest, TwoApexes) {
  MakeSdkVersion("a", 3);
  MakeSdkVersion("b", 5);

  android::derivesdk::SetSdkLevels(dir());

  EXPECT_R(3);
  EXPECT_S(3);
}

int main(int argc, char **argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}

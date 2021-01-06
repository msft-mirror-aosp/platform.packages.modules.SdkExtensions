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

#include "derive_sdk.h"

#include <android-base/properties.h>
#include <gtest/gtest.h>

#include <cstdlib>

class DeriveSdkTest : public ::testing::Test {
 protected:
  void TearDown() override { android::derivesdk::SetSdkLevels("/apex"); }
};

int R() {
  return android::base::GetIntProperty("build.version.extensions.r", -1);
}

TEST_F(DeriveSdkTest, CurrentSystemImageValue) { EXPECT_EQ(R(), 0); }

int main(int argc, char **argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}

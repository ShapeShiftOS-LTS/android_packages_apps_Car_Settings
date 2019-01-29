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

package com.android.car.settings.testutils;

import android.annotation.Nullable;
import android.app.admin.DevicePolicyManager;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.Resetter;

import java.util.List;

@Implements(value = DevicePolicyManager.class)
public class ShadowDevicePolicyManager extends org.robolectric.shadows.ShadowDevicePolicyManager {
    @Nullable
    private static List<String> sPermittedInputMethods;

    @Implementation
    @Nullable
    protected List<String> getPermittedInputMethodsForCurrentUser() {
        return sPermittedInputMethods;
    }

    public static void setPermittedInputMethodsForCurrentUser(@Nullable List<String> inputMethods) {
        sPermittedInputMethods = inputMethods;
    }

    @Resetter
    public static void reset() {
        sPermittedInputMethods = null;
    }
}

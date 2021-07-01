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

package com.android.car.settings.enterprise;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.os.UserHandle;
import android.text.TextUtils;

import androidx.preference.Preference;

import com.android.car.settings.common.FragmentController;

/**
 * Controller for the support info preference the device admin details screen.
 */
public final class DeviceAdminAddSupportPreferenceController
        extends BaseDeviceAdminAddPreferenceController<Preference> {

    public DeviceAdminAddSupportPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController, CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
    }

    @Override
    protected void updateState(Preference preference) {
        CharSequence supportMessage = mDpm.getLongSupportMessageForUser(
                mDeviceAdminInfo.getComponent(), UserHandle.myUserId());
        if (TextUtils.isEmpty(supportMessage)) {
            mLogger.v("no support message");
            return;
        }
        preference.setTitle(supportMessage);
    }
}

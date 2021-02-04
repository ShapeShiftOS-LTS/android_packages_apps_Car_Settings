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

package com.android.car.settings.wifi;

import android.car.drivingstate.CarUxRestrictions;
import android.content.Context;
import android.net.wifi.WifiManager;

import com.android.car.settings.R;
import com.android.car.settings.common.ColoredSwitchPreference;
import com.android.car.settings.common.FragmentController;
import com.android.car.settings.common.PreferenceController;

/**
 * Enables/disables Wifi state via SwitchPreference.
 */
public class WifiStateSwitchPreferenceController extends
        PreferenceController<ColoredSwitchPreference>
        implements CarWifiManager.Listener {

    private final CarWifiManager mCarWifiManager;

    public WifiStateSwitchPreferenceController(Context context, String preferenceKey,
            FragmentController fragmentController,
            CarUxRestrictions uxRestrictions) {
        super(context, preferenceKey, fragmentController, uxRestrictions);
        mCarWifiManager = new CarWifiManager(context);
    }

    @Override
    protected Class<ColoredSwitchPreference> getPreferenceType() {
        return ColoredSwitchPreference.class;
    }

    @Override
    protected void updateState(ColoredSwitchPreference preference) {
        updateSwitchPreference(preference, mCarWifiManager.isWifiEnabled());
    }

    @Override
    protected boolean handlePreferenceChanged(ColoredSwitchPreference preference, Object newValue) {
        boolean wifiEnabled = (Boolean) newValue;
        mCarWifiManager.setWifiEnabled(wifiEnabled);
        return true;
    }

    @Override
    protected void onCreateInternal() {
        getPreference().setContentDescription(
                getContext().getString(R.string.wifi_state_switch_content_description));
    }

    @Override
    protected void onStartInternal() {
        mCarWifiManager.addListener(this);
        mCarWifiManager.start();
        onWifiStateChanged(mCarWifiManager.getWifiState());
    }

    @Override
    protected void onStopInternal() {
        mCarWifiManager.removeListener(this);
        mCarWifiManager.stop();
    }

    @Override
    protected void onDestroyInternal() {
        mCarWifiManager.destroy();
    }

    @Override
    public void onAccessPointsChanged() {
        // intentional no-op
    }

    @Override
    public void onWifiStateChanged(int state) {
        updateSwitchPreference(getPreference(), state == WifiManager.WIFI_STATE_ENABLED
                || state == WifiManager.WIFI_STATE_ENABLING);
    }

    private void updateSwitchPreference(ColoredSwitchPreference preference, boolean enabled) {
        preference.setTitle(enabled ? R.string.car_ui_preference_switch_on
                : R.string.car_ui_preference_switch_off);
        preference.setChecked(enabled);
    }
}
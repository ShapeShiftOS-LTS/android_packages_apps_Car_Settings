/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.settings.security;

import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.VisibleForTesting;

import com.android.car.settings.R;
import com.android.car.settings.common.BaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for confirming existing lock PIN or password.  The containing activity must implement
 * CheckLockListener.
 */
public class ConfirmLockPinPasswordFragment extends BaseFragment {

    private static final String FRAGMENT_TAG_CHECK_LOCK_WORKER = "check_lock_worker";
    private static final String EXTRA_IS_PIN = "extra_is_pin";

    // Number of keys in the pin pad, 0-9 plus backspace and enter keys.
    private static final int NUM_KEYS = 12;

    private EditText mPasswordField;
    private TextView mMsgView;
    private final List<View> mPinKeys = new ArrayList<>(NUM_KEYS);

    private CheckLockWorker mCheckLockWorker;
    private CheckLockListener mCheckLockListener;

    private int mUserId;
    private boolean mIsPin;
    private String mEnteredPassword;

    /**
     * Factory method for creating fragment in PIN mode.
     */
    public static ConfirmLockPinPasswordFragment newPinInstance() {
        ConfirmLockPinPasswordFragment patternFragment = new ConfirmLockPinPasswordFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.confirm_lock_pin_fragment);
        bundle.putBoolean(EXTRA_IS_PIN, true);
        patternFragment.setArguments(bundle);
        return patternFragment;
    }

    /**
     * Factory method for creating fragment in password mode.
     */
    public static ConfirmLockPinPasswordFragment newPasswordInstance() {
        ConfirmLockPinPasswordFragment patternFragment = new ConfirmLockPinPasswordFragment();
        Bundle bundle = BaseFragment.getBundle();
        bundle.putInt(EXTRA_TITLE_ID, R.string.security_settings_title);
        bundle.putInt(EXTRA_ACTION_BAR_LAYOUT, R.layout.action_bar_with_button);
        bundle.putInt(EXTRA_LAYOUT, R.layout.confirm_lock_password_fragment);
        bundle.putBoolean(EXTRA_IS_PIN, false);
        patternFragment.setArguments(bundle);
        return patternFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if ((getActivity() instanceof CheckLockListener)) {
            mCheckLockListener = (CheckLockListener) getActivity();
        } else {
            throw new RuntimeException("The activity must implement CheckLockListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserId = UserHandle.myUserId();
        Bundle args = getArguments();
        if (args != null) {
            mIsPin = args.getBoolean(EXTRA_IS_PIN);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPasswordField = (EditText) view.findViewById(R.id.password_entry);
        mMsgView = (TextView) view.findViewById(R.id.message);

        if (mIsPin) {
            initPinView(view);
        } else {
            initPasswordView();
        }

        if (savedInstanceState != null) {
            mCheckLockWorker = (CheckLockWorker) getFragmentManager().findFragmentByTag(
                    FRAGMENT_TAG_CHECK_LOCK_WORKER);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mCheckLockWorker != null) {
            mCheckLockWorker.setListener(this::onCheckCompleted);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mCheckLockWorker != null) {
            mCheckLockWorker.setListener(null);
        }
    }

    private void initCheckLockWorker() {
        if (mCheckLockWorker == null) {
            mCheckLockWorker = new CheckLockWorker();
            mCheckLockWorker.setListener(this::onCheckCompleted);

            getFragmentManager()
                    .beginTransaction()
                    .add(mCheckLockWorker, FRAGMENT_TAG_CHECK_LOCK_WORKER)
                    .commitNow();
        }
    }

    private void initPinView(View view) {
        for (int keyId : PasswordHelper.PIN_PAD_DIGIT_KEYS) {
            TextView key = view.findViewById(keyId);
            String digit = key.getTag().toString();
            key.setOnClickListener(v -> {
                clearError();
                mPasswordField.append(digit);
            });
            mPinKeys.add(key);
        }

        View backspace = view.findViewById(R.id.key_backspace);
        backspace.setOnClickListener(v -> {
            clearError();
            String pin = mPasswordField.getText().toString();
            if (pin.length() > 0) {
                mPasswordField.setText(pin.substring(0, pin.length() - 1));
            }
        });
        mPinKeys.add(backspace);

        View enter = view.findViewById(R.id.key_enter);
        enter.setOnClickListener(v -> {
            initCheckLockWorker();
            setPinPadEnabled(false);
            mEnteredPassword = mPasswordField.getText().toString();
            mCheckLockWorker.checkPinPassword(mUserId, mEnteredPassword);
        });
        mPinKeys.add(enter);

    }

    private void initPasswordView() {
        mPasswordField.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            // Check if this was the result of hitting the enter or "done" key.
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {

                initCheckLockWorker();
                if (!mCheckLockWorker.isCheckInProgress()) {
                    mEnteredPassword = mPasswordField.getText().toString();
                    mCheckLockWorker.checkPinPassword(mUserId, mEnteredPassword);
                }
                return true;
            }
            return false;
        });

        mPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                clearError();
            }
        });
    }

    private void setPinPadEnabled(boolean enabled) {
        for (View key: mPinKeys) {
            key.setEnabled(enabled);
        }
    }

    private void clearError() {
        if (!TextUtils.isEmpty(mMsgView.getText())) {
            mMsgView.setText("");
        }
    }

    private void hideKeyboard() {
        View currentFocus = getActivity().getCurrentFocus();
        if (currentFocus == null) {
            currentFocus = getActivity().getWindow().getDecorView();
        }

        if (currentFocus != null) {
            InputMethodManager inputMethodManager =
                    (InputMethodManager) currentFocus.getContext()
                            .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager
                    .hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    void onCheckCompleted(boolean lockMatched) {
        if (lockMatched) {
            mCheckLockListener.onLockVerified(mEnteredPassword);
        } else {
            mMsgView.setText(
                    mIsPin ? R.string.lockscreen_wrong_pin : R.string.lockscreen_wrong_password);
            setPinPadEnabled(true);
        }

        if (!mIsPin) {
            hideKeyboard();
        }
    }
}

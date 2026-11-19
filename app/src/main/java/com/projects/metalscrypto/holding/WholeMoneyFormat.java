package com.projects.metalscrypto.holding;

import android.content.SharedPreferences;

import java.math.RoundingMode;
import java.text.DecimalFormat;

final class WholeMoneyFormat {
    static final String PREFERENCE_KEY = "pref_whole_dollars_only";

    private WholeMoneyFormat() {
    }

    static boolean wholeDollarsOnly(SharedPreferences preferences) {
        return preferences.getBoolean(PREFERENCE_KEY, true);
    }

    static DecimalFormat newFormatter(boolean wholeDollarsOnly) {
        DecimalFormat format = new DecimalFormat(wholeDollarsOnly ? "#,##0" : "#,##0.00");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}

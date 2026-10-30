package com.homik.bell;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;

final class AppPalette {
    static final int THEME_SYSTEM = -1;
    static final int THEME_LIGHT = 0;
    static final int THEME_DARK = 1;

    final int background, surface, text, muted, accent, divider, accentContainer, onAccentContainer;
    final boolean dark;

    private AppPalette(Activity activity) {
        SharedPreferences prefs = activity.getSharedPreferences("settings", Context.MODE_PRIVATE);
        int mode = prefs.getInt("themeMode", prefs.contains("darkTheme")
                ? (prefs.getBoolean("darkTheme", false) ? THEME_DARK : THEME_LIGHT) : THEME_SYSTEM);
        dark = mode == THEME_DARK || (mode == THEME_SYSTEM &&
                (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);

        int defaultBackground = dark ? 0xFF12131B : 0xFFF4F5FA;
        int defaultSurface = dark ? 0xFF1C1F2B : 0xFFFFFFFF;
        int defaultText = dark ? 0xFFF1F2FB : 0xFF20233A;
        int defaultMuted = dark ? 0xFFA7AAC0 : 0xFF747991;
        int defaultAccent = dark ? 0xFF8B82FF : 0xFF6258E8;
        int defaultDivider = dark ? 0xFF2D3040 : 0xFFF0F1F6;
        int defaultContainer = dark ? 0xFF302E4D : 0xFFEAE9FF;
        int defaultOnContainer = dark ? 0xFFC6C1FF : defaultAccent;

        if (Build.VERSION.SDK_INT >= 31) {
            background = color(activity, dark ? "system_neutral1_900" : "system_neutral1_10", defaultBackground);
            surface = color(activity, dark ? "system_neutral1_800" : "system_neutral1_0", defaultSurface);
            text = color(activity, dark ? "system_neutral1_100" : "system_neutral1_900", defaultText);
            muted = color(activity, dark ? "system_neutral2_300" : "system_neutral2_700", defaultMuted);
            accent = color(activity, dark ? "system_accent1_200" : "system_accent1_600", defaultAccent);
            divider = color(activity, dark ? "system_neutral2_700" : "system_neutral2_100", defaultDivider);
            accentContainer = color(activity, dark ? "system_accent1_800" : "system_accent1_50", defaultContainer);
            onAccentContainer = color(activity, dark ? "system_accent1_100" : "system_accent1_800", defaultOnContainer);
        } else {
            background = defaultBackground; surface = defaultSurface; text = defaultText; muted = defaultMuted;
            accent = defaultAccent; divider = defaultDivider; accentContainer = defaultContainer; onAccentContainer = defaultOnContainer;
        }
    }

    static AppPalette from(Activity activity) { return new AppPalette(activity); }

    static int themeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        return prefs.getInt("themeMode", prefs.contains("darkTheme")
                ? (prefs.getBoolean("darkTheme", false) ? THEME_DARK : THEME_LIGHT) : THEME_SYSTEM);
    }

    static void setThemeMode(Context context, int mode) {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putInt("themeMode", mode).apply();
    }

    private static int color(Context context, String name, int fallback) {
        int id = context.getResources().getIdentifier(name, "color", "android");
        if (id == 0) return fallback;
        try { return context.getResources().getColor(id); }
        catch (RuntimeException ignored) { return fallback; }
    }

    private AppPalette() { throw new AssertionError(); }
}

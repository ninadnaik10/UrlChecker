package com.trianguloy.urlchecker.modules.companions;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.utilities.generics.GenericPref;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;

/**
 * Manages the incognito feature
 */
public class Incognito {

    /**
     * preference
     */
    public static GenericPref.Enumeration<OnOffConfig> PREF(Context cntx) {
        return new GenericPref.Enumeration<>("open_incognito", OnOffConfig.AUTO, OnOffConfig.class, cntx);
    }

    private final GenericPref.Enumeration<OnOffConfig> pref;
    private boolean state = false;

    public Incognito(Context cntx) {
        this.pref = PREF(cntx);
    }

    /**
     * Initialization from a given intent and a button to toggle
     */
    public void initFrom(Intent intent, ImageButton button) {
        // init state
        boolean visible;
        switch (pref.get()) {
            case AUTO:
            default:
                // for Firefox
                state = intent.getBooleanExtra("private_browsing_mode", false);
                visible = true;
                break;
            case HIDDEN:
                // for Firefox
                state = intent.getBooleanExtra("private_browsing_mode", false);
                visible = false;
                break;
            case DEFAULT_ON:
                state = true;
                visible = true;
                break;
            case DEFAULT_OFF:
                state = false;
                visible = true;
                break;
            case ALWAYS_ON:
                state = true;
                visible = false;
                break;
            case ALWAYS_OFF:
                state = false;
                visible = false;
                break;
        }

        // init button
        if (visible) {
            // show and configure
            button.setVisibility(View.VISIBLE);
            AndroidUtils.longTapForDescription(button);
            AndroidUtils.toggleableListener(button,
                    imageButton -> state = !state,
                    v -> v.setImageResource(state ? R.drawable.incognito : R.drawable.no_incognito)
            );
        } else {
            // hide
            button.setVisibility(View.GONE);
        }
    }

    /**
     * applies the setting to a given intent
     */
    public void apply(Intent intent) {
        // for Firefox
        intent.putExtra("private_browsing_mode", state);
    }
}

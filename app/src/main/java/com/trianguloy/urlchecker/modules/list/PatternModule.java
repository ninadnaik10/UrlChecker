package com.trianguloy.urlchecker.modules.list;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ModulesActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.companions.PatternCatalog;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.methods.AndroidUtils;
import com.trianguloy.urlchecker.utilities.methods.Inflater;
import com.trianguloy.urlchecker.utilities.methods.JavaUtils;
import com.trianguloy.urlchecker.utilities.wrappers.RegexFix;

import org.json.JSONArray;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * This module checks for patterns characters in the url
 */
public class PatternModule extends AModuleData {

    @Override
    public String getId() {
        return "pattern";
    }

    @Override
    public int getName() {
        return R.string.mPttrn_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new PatternDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ModulesActivity cntx) {
        return new PatternConfig(cntx);
    }
}

class PatternConfig extends AModuleConfig {

    private final PatternCatalog catalog;

    public PatternConfig(ModulesActivity cntx) {
        super(cntx);
        catalog = new PatternCatalog(cntx);
    }

    @Override
    public int getLayoutId() {
        return R.layout.config_patterns;
    }

    @Override
    public void onInitialize(View views) {
        views.findViewById(R.id.edit).setOnClickListener(v -> catalog.showEditor());
        views.<TextView>findViewById(R.id.user_content)
                .setText(getActivity().getString(
                        R.string.mPttrn_userContent,
                        "https://github.com/TrianguloY/UrlChecker/wiki/Custom-patterns"
                ));
        RegexFix.attachSetting(views.findViewById(R.id.regex_fix));
    }

}

class PatternDialog extends AModuleDialog {
    public static final String APPLIED = "pattern.applied";

    private TextView txt_noPatterns;
    private LinearLayout box;

    private final PatternCatalog catalog;
    private final RegexFix regexFix;

    private final List<Message> messages = new ArrayList<>();

    public PatternDialog(MainDialog dialog) {
        super(dialog);
        catalog = new PatternCatalog(dialog);
        regexFix = new RegexFix(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_pattern;
    }

    @Override
    public void onInitialize(View views) {
        txt_noPatterns = views.findViewById(R.id.pattern);
        box = views.findViewById(R.id.box);
    }

    @Override
    public void onModifyUrl(UrlData urlData, JavaUtils.Function<UrlData, Boolean> setNewUrl) {
        // init
        messages.clear();

        // check each pattern
        var patterns = catalog.getCatalog();
        for (var pattern : JavaUtils.toList(patterns.keys())) {
            var url = urlData.url;
            try {
                var data = patterns.optJSONObject(pattern);
                var message = new Message(pattern);

                // enabled?
                if (data == null) continue;
                if (!data.optBoolean("enabled", true)) continue;

                // get regex (must exists)
                if (!data.has("regex")) continue;

                // applied previously?
                message.applied = urlData.getData(APPLIED + pattern) != null;

                // encode if required
                if (data.optBoolean("encode")) {
                    url = URLEncoder.encode(url);
                }

                // check matches
                // if 'regex' matches, the pattern can match
                // if 'regex' doesn't match, the patter doesn't match
                var regex_matcher = Pattern.compile(data.getString("regex")).matcher(url);
                var matches = regex_matcher.find();
                if (matches && data.has("excludeRegex")) {
                    // if 'excludeRegex' doesn't exist, the pattern can match
                    // if 'excludeRegex' matches, the pattern doesn't matches
                    // if 'excludeRegex' doesn't match, the pattern can match
                    matches = !Pattern.compile(data.getString("excludeRegex")).matcher(url).find();
                }
                if (matches) {
                    message.matches = true;

                    // check replacements
                    String replacement = null;

                    var replacements = data.opt("replacement");
                    if (replacements != null) {
                        // data exists
                        if (replacements instanceof JSONArray) {
                            // array, get random
                            var replacementsArray = (JSONArray) replacements;
                            replacement = replacementsArray.getString(new Random().nextInt(replacementsArray.length()));
                        } else {
                            // single data, get that one
                            replacement = replacements.toString();
                        }
                    }

                    if (replacement != null) {
                        // replace url
                        message.newUrl = regexFix.replaceAll(url, regex_matcher, replacement);

                        // decode if required
                        if (data.optBoolean("decode")) {
                            message.newUrl = URLDecoder.decode(message.newUrl);
                        }

                        // automatic? apply
                        if (data.optBoolean("automatic")) {
                            if (setNewUrl.apply(new UrlData(message.newUrl).putData(APPLIED + pattern, APPLIED))) return;
                        }
                    }
                }


                // add
                if (message.applied || message.matches) messages.add(message);

            } catch (Exception e) {
                // invalid pattern? ignore
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisplayUrl(UrlData urlData) {
        // visualize
        box.removeAllViews();
        if (messages.isEmpty()) {
            // no messages, all good
            txt_noPatterns.setVisibility(View.VISIBLE);
            setVisibility(false);
        } else {
            // messages to show, set them
            txt_noPatterns.setVisibility(View.GONE);
            setVisibility(true);

            for (Message message : messages) {
                // either matches and/or applied is true
                View row = Inflater.inflate(R.layout.button_text, box);

                // text
                TextView text = row.findViewById(R.id.text);
                text.setText(message.applied
                        ? getActivity().getString(R.string.mPttrn_fixed, message.pattern)
                        : message.pattern
                );
                AndroidUtils.setRoundedColor(message.matches ? R.color.warning : R.color.good, text);

                // button
                Button fix = row.findViewById(R.id.button);
                fix.setText(R.string.mPttrn_fix);
                fix.setEnabled(message.newUrl != null);
                if (message.newUrl != null) fix.setOnClickListener(v -> setUrl(new UrlData(message.newUrl).putData(APPLIED + message.pattern, APPLIED)));
            }
        }
    }

    /**
     * DataClass for pattern messages
     */
    private static class Message {
        final String pattern;
        boolean applied;
        public boolean matches;
        String newUrl;

        public Message(String pattern) {
            this.pattern = pattern;
        }
    }
}

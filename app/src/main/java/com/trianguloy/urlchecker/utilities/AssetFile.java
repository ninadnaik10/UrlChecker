package com.trianguloy.urlchecker.utilities;

import android.content.Context;

import java.io.IOException;

/**
 * Represents a file from assets (read-only)
 */
public class AssetFile {
    private final String fileName;
    private Context cntx;

    public AssetFile(String fileName) {
        this.fileName = fileName;
    }

    public void init(Context cntx) {
        this.cntx = cntx;
    }

    /**
     * Returns the content of the file, or null if can't be read)
     */
    public String get() {
        // get the updated file first
        try {
            return StreamUtils.inputStream2String(cntx.getAssets().open(fileName));
        } catch (IOException ignored) {
            return null;
        }
    }

}

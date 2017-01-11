package com.test.draganddroptask;

import android.net.Uri;

public class Constants {
    static public final String DATA_ID = "_id";
    static public final String DATA_TITLE = "title";
    static public final String DATA_NEXT = "next";
    static public final String DATA_PREV = "prev";
    static public final String AUTHORITY = "com.test.draganddroptask.TestData";
    static public final String DATA_PATH = "data";
    static public final Uri DATA_CONTENT_URI = Uri.parse("content://"
            + AUTHORITY + "/" + DATA_PATH);
}

package com.holaspark.holaplayer.internal;
import android.net.Uri;
import com.google.android.exoplayer2.C;
public class ContentTypeDetector {
static int detect_source(Uri uri)
{
    int type = C.TYPE_OTHER;
    String path = uri.getPath();
    if (path != null)
    {
        path = path.toLowerCase();
        if (path.endsWith(".m3u8"))
            type = C.TYPE_HLS;
        if (path.endsWith(".mpd"))
            type = C.TYPE_DASH;
    }
    return type;
}
}

package com.holaspark.holaplayer;
import android.net.Uri;
public class PlayItem {
private final String m_ad_tag;
private final String m_media;
public PlayItem(String ad_tag, String media){
    m_ad_tag = ad_tag;
    m_media = media;
}
public String get_ad_tag(){ return m_ad_tag; }
public String get_media(){ return m_media; }
}

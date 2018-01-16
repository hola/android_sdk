package com.holaspark.holaplayer;

public class ThumbnailsConfig {
public int m_width;
public int m_height;
public int m_group_size;
public int m_interval;
public String[] m_cdns;
public String[] m_urls;

public ThumbnailsConfig(int width, int height, int group_size, int interval, String[] cdns,
    String[] urls){
    m_width = width;
    m_height = height;
    m_group_size = group_size;
    m_interval = interval;
    m_cdns = cdns;
    m_urls = urls;
}
}


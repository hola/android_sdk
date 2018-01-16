package com.holaspark.holaplayer;
public interface HolaPlayerAPI {
void play();
void pause();
void seek(long position);
void load(String url);
void queue(PlayItem item);
void uninit();
void fullscreen(Boolean state);
void float_mode(Boolean state);
void vr_mode(Boolean state);
void set_customer(String m_customer);
void set_watch_next_items(PlayListItem[] items);
}

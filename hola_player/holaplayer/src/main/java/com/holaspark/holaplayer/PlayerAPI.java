package com.holaspark.holaplayer;
public interface PlayerAPI {
void play();
void pause();
void seek(long position);
void load(String url);
void queue(PlayItem item);
void uninit();
void fullscreen(Boolean state);
void float_mode(Boolean state);
}

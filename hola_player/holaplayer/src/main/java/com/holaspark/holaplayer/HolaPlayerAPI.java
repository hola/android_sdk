package com.holaspark.holaplayer;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;

public interface HolaPlayerAPI {
void play();
void pause();
boolean is_playing();
boolean is_playing_ad();
boolean is_paused();
int get_playback_state();
void seek(long position);
long get_position();
long get_duration();
void load(String url);
void queue(PlayItem item);
void uninit();
void fullscreen(Boolean state);
void float_mode(Boolean state);
void vr_mode(Boolean state);
void set_customer(String m_customer);
void set_watch_next_items(PlayListItem[] items);
void add_listener(EventListener listener);
void remove_listener(EventListener listener);
int get_video_width();
int get_video_height();
void set_controls_state(boolean enabled);
boolean get_controls_state();
void set_controls_visibility(boolean visible);
boolean get_controls_visibility();
boolean get_vr_mode();

interface EventListener {
    void on_play();
    void on_pause();
    void on_state_changed(int playback_state);
    void on_seeked();
    void on_error(ExoPlaybackException error);
    void on_timeline_changed(Timeline timeline, Object manifest);
    void on_tracks_changed(TrackGroupArray track_groups, TrackSelectionArray track_selections);
    void on_loading_changed(boolean is_loading);
    void on_position_discontinuity(@Player.DiscontinuityReason int reason);
    void on_playback_parameters_changed(PlaybackParameters playback_parameters);
    void on_ad_start();
    void on_ad_end();
    void on_fullscreen_changed(boolean is_fullscreen);
}

abstract class DefaultEventListener implements EventListener {
    @Override
    public void on_play(){}
    @Override
    public void on_pause(){}
    @Override
    public void on_state_changed(int playback_state){}
    @Override
    public void on_seeked(){}
    @Override
    public void on_error(ExoPlaybackException error){}
    @Override
    public void on_timeline_changed(Timeline timeline, Object manifest){}
    @Override
    public void on_tracks_changed(TrackGroupArray track_groups, TrackSelectionArray track_selections){}
    @Override
    public void on_loading_changed(boolean is_loading){}
    @Override
    public void on_position_discontinuity(@Player.DiscontinuityReason int reason){}
    @Override
    public void on_playback_parameters_changed(PlaybackParameters playback_parameters){}
    @Override
    public void on_ad_start(){}
    @Override
    public void on_ad_end(){}
    @Override
    public void on_fullscreen_changed(boolean is_fullscreen){}
}
}
// Copyright 2014 Google Inc. All Rights Reserved.

package com.google.ads.interactivemedia.v3.samples.samplevideoplayer;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.util.AttributeSet;
import android.widget.MediaController;
import android.widget.VideoView;

import java.util.ArrayList;
import java.util.List;

/**
 * A VideoView that intercepts various methods and reports them back via a
 * PlayerCallback.
 */
public class SampleVideoPlayer implements VideoPlayer {

    private VideoView m_vview;

    private enum PlaybackState {
        STOPPED, PAUSED, PLAYING
    }

    private MediaController mMediaController;
    private PlaybackState mPlaybackState;
    private final List<PlayerCallback> mVideoPlayerCallbacks = new ArrayList<PlayerCallback>(1);

    public SampleVideoPlayer(VideoView source, Context ctx) {
        m_vview = source;
        init(ctx);
    }

    private void init(Context ctx) {
        mPlaybackState = PlaybackState.STOPPED;
        mMediaController = new MediaController(ctx);
        mMediaController.setAnchorView(m_vview);

        // Set OnCompletionListener to notify our callbacks when the video is completed.
        m_vview.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                // Reset the MediaPlayer.
                // This prevents a race condition which occasionally results in the media
                // player crashing when switching between videos.
                mediaPlayer.reset();
                mediaPlayer.setDisplay(m_vview.getHolder());
                mPlaybackState = PlaybackState.STOPPED;

                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onCompleted();
                }
            }
        });

        // Set OnErrorListener to notify our callbacks if the video errors.
        m_vview.setOnErrorListener(new OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mPlaybackState = PlaybackState.STOPPED;
                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onError();
                }

                // Returning true signals to MediaPlayer that we handled the error. This will
                // prevent the completion handler from being called.
                return true;
            }
        });
    }

    // Methods implementing the VideoPlayer interface.

    @Override
    public void play() {
        start();
    }

    public void start() {
        m_vview.start();
        m_vview.setMediaController(mMediaController);
        // Fire callbacks before switching playback state.
        switch (mPlaybackState) {
            case STOPPED:
                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onPlay();
                }
                break;
            case PAUSED:
                for (PlayerCallback callback : mVideoPlayerCallbacks) {
                    callback.onResume();
                }
                break;
            default:
                // Already playing; do nothing.
        }
        mPlaybackState = PlaybackState.PLAYING;
    }

    @Override
    public void stopPlayback() {
        m_vview.stopPlayback();
        m_vview.setMediaController(null);
        mPlaybackState = PlaybackState.STOPPED;
    }

    @Override
    public void setVideoPath(String videoUrl){
        m_vview.setVideoPath(videoUrl); }

    @Override
    public void pause() {
        m_vview.pause();
        mPlaybackState = PlaybackState.PAUSED;
        for (PlayerCallback callback : mVideoPlayerCallbacks) {
            callback.onPause();
        }
    }
    @Override
    public int getCurrentPosition(){ return m_vview.getCurrentPosition(); }
    @Override
    public void seekTo(int videoPosition){ m_vview.seekTo(videoPosition); }
    @Override
    public int getDuration(){ return m_vview.getDuration(); }
    @Override
    public void addPlayerCallback(PlayerCallback callback) {
        mVideoPlayerCallbacks.add(callback);
    }
}

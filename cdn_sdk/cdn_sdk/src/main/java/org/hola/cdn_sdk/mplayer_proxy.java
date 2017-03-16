package org.hola.cdn_sdk;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Surface;
import android.view.SurfaceHolder;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Map;
public class mplayer_proxy extends MediaPlayer implements proxy_api {
private MediaPlayer m_player;
private Handler m_handler;
private String m_state = "NONE";
private boolean m_prepared = false;
private String m_videourl;
private OnPreparedListener m_prepared_listener = null;
private OnCompletionListener m_completion_listener = null;
private OnSeekCompleteListener m_seek_listener = null;
private int m_bitrate;
private int m_bandwidth;
private thread_t m_timeupdate = new thread_t();
private service m_service;
private class thread_t implements Runnable {
    private volatile Thread executor;
    public void start(){
        executor = new Thread(this);
        executor.start();
    }
    public void stop(){
        executor = null; }
    @Override
    public void run(){
        Thread _this = Thread.currentThread();
        while (_this==executor)
        {
            m_handler.sendEmptyMessage(service.MSG_TIMEUPDATE);
            try { Thread.sleep(250); }
            catch(InterruptedException e){}
        }
    }
}
public void init(Object source, Handler hnd, service service){
    m_service = service;
    m_player = (MediaPlayer) source;
    m_handler = hnd;
    update_state(m_player.isPlaying() ? "PLAYING" : "IDLE");
    m_player.setOnPreparedListener(new OnPreparedListener(){
        @Override
        public void onPrepared(MediaPlayer m_player){
            mplayer_proxy.this.m_prepared = true;
            if (m_prepared_listener!=null)
                m_prepared_listener.onPrepared(m_player);
        }
    });
    m_player.setOnSeekCompleteListener(new OnSeekCompleteListener(){
        @Override
        public void onSeekComplete(MediaPlayer player)
        {
            mplayer_proxy.this.update_state("SEEKED");
            if (m_seek_listener!=null)
                m_seek_listener.onSeekComplete(player);
        }
    });
    m_player.setOnCompletionListener(new OnCompletionListener(){
        @Override
        public void onCompletion(MediaPlayer m_player){
            if (!mplayer_proxy.this.m_prepared)
                return;
            mplayer_proxy.this.update_state("IDLE");
            if (m_completion_listener!=null)
                m_completion_listener.onCompletion(m_player);
        }
    });
}
public String get_state(){ return m_state; }
public String get_url(){ return m_videourl; }
public boolean is_prepared(){ return m_prepared; }
private void update_state(String new_state){ update_state(new_state, -1); }
private void update_state(String new_state, int param){
    Message msg = new Message();
    Bundle state_data = new Bundle();
    msg.what = service.MSG_STATE;
    msg.arg1 = param;
    state_data.putString("old", m_state);
    state_data.putString("new", new_state);
    msg.setData(state_data);
    m_state = new_state;
    m_handler.sendMessage(msg);
    if (new_state.equals("STARTING"))
        m_timeupdate.start();
    if (new_state.equals("IDLE"))
        m_timeupdate.stop();
}
public int get_bitrate(){ return m_bitrate; }
public void set_bitrate(int br){ m_bitrate = br; }
public int get_bandwidth(){ return m_bandwidth; }
public void set_bandwidth(int br){ m_bandwidth = br; }
public String get_player_name(){ return "MediaPlayer"; }
@Override
public void setDisplay(SurfaceHolder sh){ m_player.setDisplay(sh); }
@Override
public void setSurface(Surface surface){ m_player.setSurface(surface); }
@Override
public void setVideoScalingMode(int mode){
    m_player.setVideoScalingMode(mode); }
@Override
public void setDataSource(Context context, Uri uri) throws IOException,
    IllegalArgumentException, SecurityException, IllegalStateException
{
    m_videourl = uri.toString();
    m_player.setDataSource(service.mangle_request(uri));
}
@Override
public void setDataSource(Context context, Uri uri,
    Map<String, String> headers) throws IOException, IllegalArgumentException,
    SecurityException, IllegalStateException
{
    m_videourl = uri.toString();
    m_player.setDataSource(service.mangle_request(uri));
}
@Override
public void setDataSource(String path) throws IOException,
    IllegalArgumentException, SecurityException, IllegalStateException
{
    m_videourl = path;
    m_player.setDataSource(service.mangle_request(Uri.parse(path)));
}
@Override
public void setDataSource(FileDescriptor fd) throws IOException,
    IllegalArgumentException, IllegalStateException
{
    m_player.setDataSource(fd);
}
@Override
public void setDataSource(FileDescriptor fd, long offset, long length)
    throws IOException, IllegalArgumentException, IllegalStateException
{
    m_player.setDataSource(fd, offset, length);
}
@Override
public void prepare() throws IOException, IllegalStateException
{
    m_player.prepareAsync();
}
@Override
public void prepareAsync() throws IllegalStateException
{
    m_player.prepareAsync();
    update_state("STARTING");
}
@Override
public void start() throws IllegalStateException
{
    m_player.start();
    update_state("PLAYING");
}
@Override
public void stop() throws IllegalStateException
{
    m_player.stop();
    update_state("IDLE");
}
@Override
public void pause() throws IllegalStateException
{
    m_player.pause();
    update_state("PAUSED");
}
@Override
public void setWakeMode(Context context, int mode){
    m_player.setWakeMode(context, mode); }
@Override
public void setScreenOnWhilePlaying(boolean screenOn){
    m_player.setScreenOnWhilePlaying(screenOn); }
@Override
public int getVideoWidth(){ return m_player.getVideoWidth(); }
@Override
public int getVideoHeight(){ return m_player.getVideoHeight(); }
@Override
public boolean isPlaying(){ return m_player !=null && m_player.isPlaying(); }
@Override
public void seekTo(int i) throws IllegalStateException
{
    update_state("SEEKING", i);
    m_player.seekTo(i);
}
@Override
public int getCurrentPosition(){
    return m_player==null ? 0 : m_player.getCurrentPosition(); }
@Override
public int getDuration(){ return m_player.getDuration(); }
@Override
public void setNextMediaPlayer(MediaPlayer mediaPlayer){
    m_player.setNextMediaPlayer(mediaPlayer); }
@Override
public void release(){ m_player.release(); }
@Override
public void reset(){ m_player.reset(); }
@Override
public void setAudioStreamType(int streamtype){
    m_player.setAudioStreamType(streamtype); }
@Override
public void setLooping(boolean b){ m_player.setLooping(b); }
@Override
public boolean isLooping(){ return m_player.isLooping(); }
@Override
public void setVolume(float leftVolume, float rightVolume){
    m_player.setVolume(leftVolume, rightVolume); }
@Override
public void setAudioSessionId(int i) throws IllegalArgumentException,
    IllegalStateException
{
    m_player.setAudioSessionId(i);
}
@Override
public int getAudioSessionId(){ return m_player.getAudioSessionId(); }
@Override
public void attachAuxEffect(int i){ m_player.attachAuxEffect(i); }
@Override
public void setAuxEffectSendLevel(float level){
    m_player.setAuxEffectSendLevel(level); }
@Override
public TrackInfo[] getTrackInfo() throws IllegalStateException
{
    return m_player.getTrackInfo();
}
@Override
public void addTimedTextSource(String path, String mimeType)
    throws IOException, IllegalArgumentException, IllegalStateException
{
    m_player.addTimedTextSource(path, mimeType);
}
@Override
public void addTimedTextSource(Context context, Uri uri, String mimeType)
    throws IOException, IllegalArgumentException, IllegalStateException
{
    m_player.addTimedTextSource(context, uri, mimeType);
}
@Override
public void addTimedTextSource(FileDescriptor fd, String mimeType)
    throws IllegalArgumentException, IllegalStateException
{
    m_player.addTimedTextSource(fd, mimeType);
}
@Override
public void addTimedTextSource(FileDescriptor fd, long offset, long length,
    String mime) throws IllegalArgumentException, IllegalStateException
{
    m_player.addTimedTextSource(fd, offset, length, mime);
}
@Override
public void selectTrack(int index) throws IllegalStateException
{
    m_player.selectTrack(index);
}
@Override
public void deselectTrack(int index) throws IllegalStateException
{
    m_player.deselectTrack(index);
}
@Override
public String get_buffered(){ return m_service.get_buffered(); }
@Override
public void setOnPreparedListener(OnPreparedListener listener){
    m_prepared_listener = listener; }
@Override
public void setOnCompletionListener(OnCompletionListener listener){
    m_completion_listener = listener; }
@Override
public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener){
    m_player.setOnBufferingUpdateListener(listener); }
@Override
public void setOnSeekCompleteListener(OnSeekCompleteListener listener){
    m_seek_listener = listener; }
@Override
public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener){
    m_player.setOnVideoSizeChangedListener(listener); }
@Override
public void setOnTimedTextListener(OnTimedTextListener listener){
    m_player.setOnTimedTextListener(listener); }
@Override
public void setOnErrorListener(OnErrorListener listener){
    m_player.setOnErrorListener(listener); }
@Override
public void setOnInfoListener(OnInfoListener listener){
    m_player.setOnInfoListener(listener); }
}

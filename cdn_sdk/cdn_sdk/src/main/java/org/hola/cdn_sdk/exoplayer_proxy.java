package org.hola.cdn_sdk;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
public class exoplayer_proxy implements proxy_api, ExoPlayer.Listener {
private final String m_url;
private ExoPlayer m_player;
private int m_bitrate;
private int m_bandwidth;
private Handler m_handler;
private String m_state = "NONE";
private thread_t m_timeupdate = new thread_t();
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
exoplayer_proxy(String url){ this.m_url = url; }
@Override
public int getDuration(){ return (int) m_player.getDuration(); }
@Override
public int getCurrentPosition(){ return (int) m_player.getCurrentPosition(); }
@Override
public String get_url(){ return m_url; }
@Override
public int get_bitrate(){ return m_bitrate; }
@Override
public int get_bandwidth(){ return m_bandwidth; }
@Override
public String get_state(){ return m_state; }
@Override
public boolean is_prepared(){
    int state = m_player.getPlaybackState();
    return state!=ExoPlayer.STATE_IDLE&&state!=ExoPlayer.STATE_PREPARING;
}
@Override
public void set_bitrate(int br){ m_bitrate = br; }
@Override
public void set_bandwidth(int br){ m_bandwidth = br; }
@Override
public void init(Object source, Handler handler, service service){
    this.m_player = (ExoPlayer) source;
    this.m_player.addListener(this);
    this.m_handler = handler;
    update_state(is_prepared()&&m_player.getPlayWhenReady() ?
        "PLAYING" : "IDLE");
}
@Override
public String get_buffered(){
    return "[["+(m_player.getCurrentPosition()/1000-1)+','
        +(m_player.getBufferedPosition()/1000)+"]]";
}
@Override
public String get_player_name(){ return "ExoPlayer"; }
@Override
public void onPlayerStateChanged(boolean play, int playbackState){
    String newstate="";
    newstate = play ? "PLAYING" : "PAUSED";
    switch (playbackState)
    {
    case ExoPlayer.STATE_PREPARING: newstate = "STARTING"; break;
    case ExoPlayer.STATE_ENDED: newstate = "IDLE"; break;
    }
    if (!newstate.equals(m_state))
        update_state(newstate);
}
@Override
public void onPlayWhenReadyCommitted(){}
@Override
public void onPlayerError(ExoPlaybackException e){}
}

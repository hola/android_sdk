package org.hola.cdn_sdk;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.JavascriptInterface;
@SuppressWarnings("unused")
public class js_proxy {
private final service m_service;
private final Handler m_handler;
private mplayer_proxy m_player;
private int m_reqid = 1;
public js_proxy(Handler handler, service service){
    m_handler = handler;
    m_service = service;
}
public void set_player(mplayer_proxy player){ m_player = player; }
@JavascriptInterface
public int get_duration(){ return m_player.getDuration(); }
@JavascriptInterface
public int get_pos(){ return m_player.getCurrentPosition(); }
@JavascriptInterface
public String get_url(){ return m_player.get_url(); }
@JavascriptInterface
public int get_bitrate(){ return m_player.get_bitrate(); }
@JavascriptInterface
public int get_bandwidth(){ return m_player.get_bandwidth(); }
@JavascriptInterface
public String get_levels(){ return m_service.get_levels(); }
@JavascriptInterface
public String get_segment_info(String url){
    return m_service.get_segment_info(url); }
@JavascriptInterface
public String get_state(){ return m_player.get_state(); }
// XXX pavelki: stub
@JavascriptInterface
public int get_buffered(){ return 0; }
@JavascriptInterface
public boolean is_prepared(){ return m_player.is_prepared(); }
@JavascriptInterface
public int fetch(String url, int frag_id){
    Bundle bundle = new Bundle();
    bundle.putString("url", url);
    Message msg = new Message();
    msg.what = service.MSG_RESPONSE;
    msg.arg1 = frag_id;
    msg.arg2 = m_reqid;
    msg.setData(bundle);
    m_handler.sendMessage(msg);
    return m_reqid++;
}
@JavascriptInterface
public void fetch_remove(int req_id){
    Message msg = new Message();
    msg.what = service.MSG_REMOVE;
    msg.arg1 = req_id;
    m_handler.sendMessage(msg);
}
@JavascriptInterface
public void fragment_data(int frag_id, int req_id){
    Message msg = new Message();
    msg.what = service.MSG_FRAGMENT;
    msg.arg1 = frag_id;
    msg.arg2 = req_id;
    m_handler.sendMessage(msg);
}
@JavascriptInterface
public void wrapper_attached(){
    m_handler.sendEmptyMessage(service.MSG_ATTACHED); }
}

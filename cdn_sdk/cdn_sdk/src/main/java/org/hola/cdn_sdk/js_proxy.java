package org.hola.cdn_sdk;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.webkit.JavascriptInterface;
@SuppressWarnings("unused")
public class js_proxy {
private final service m_service;
private final Handler m_handler;
private proxy_api m_proxy;
private int m_reqid = 1;
public js_proxy(Handler handler, service service){
    m_handler = handler;
    m_service = service;
}
public void set_proxy(proxy_api proxy){ m_proxy = proxy; }
@JavascriptInterface
public int get_duration(){ return m_proxy.getDuration(); }
@JavascriptInterface
public int get_pos(){ return m_proxy.getCurrentPosition(); }
@JavascriptInterface
public String get_url(){ return m_proxy.get_url(); }
@JavascriptInterface
public int get_bitrate(){ return m_proxy.get_bitrate(); }
@JavascriptInterface
public int get_bandwidth(){ return m_proxy.get_bandwidth(); }
@JavascriptInterface
public String get_levels(){ return m_service.get_levels(); }
@JavascriptInterface
public String get_segment_info(String url){
    return m_service.get_segment_info(url); }
@JavascriptInterface
public String get_state(){ return m_proxy.get_state(); }
@JavascriptInterface
public int get_buffered(){ return 0; }
@JavascriptInterface
public boolean is_prepared(){ return m_proxy.is_prepared(); }
@JavascriptInterface
public String get_player_name(){ return m_proxy.get_player_name(); }
@JavascriptInterface
public int fetch(String url, int frag_id){
    m_handler.sendMessage(build_fetch_msg(url, frag_id));
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
int get_new_reqid(){ return m_reqid++; }
Message build_fetch_msg(String url, int frag_id){
    Bundle bundle = new Bundle();
    bundle.putString("url", url);
    Message msg = new Message();
    msg.what = service.MSG_RESPONSE;
    msg.arg1 = frag_id;
    msg.arg2 = m_reqid;
    msg.setData(bundle);
    return msg;
}
}


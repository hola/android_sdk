package org.hola.cdn_sdk;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
@SuppressWarnings("unused")
public class api {
final static String TAG = "HolaCDN";
public final static int MSG_SERVICE_CONNECTED = 1;
public final static int MSG_WEBSOCKET_CONNECTED = 2;
public final static int MSG_TIMEUPDATE = 3;
private ServiceConnection m_conn = null;
private service m_service;
private Context m_ctx;
private Bundle m_extra;
private String m_customer;
public boolean init(Context ctx, String customer, Bundle extra,
    final Handler callback)
{
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT)
        return false;
    Log.d(TAG, "Hola CDN API init");
    m_ctx = ctx;
    m_extra = extra;
    m_customer = customer;
    m_conn = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName,
            IBinder iBinder)
        {
            Log.d(TAG, "Hola CDN API connected to service");
            m_service = ((service.hola_service_binder) iBinder).get_service();
            m_service.init(m_customer, m_extra, callback);
            callback.sendEmptyMessage(MSG_SERVICE_CONNECTED);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName){
            Log.d(TAG, "Hola CDN API disconnected from service");
            m_service = null;
        }
    };
    m_ctx.bindService(new Intent(ctx, service.class), m_conn,
        Context.BIND_AUTO_CREATE);
    return true;
}
public MediaPlayer attach(MediaPlayer player){
    if (m_service==null)
        return null;
    return m_service.attach(player);
}
public boolean is_inited(){ return m_service!=null; }
public boolean is_connected(){ return m_service!=null && m_service.is_ws(); }
public boolean is_attached(){
    return m_service!=null && m_service.is_attached(); }
public void send_message(String msg, String data){
    m_service.send_message(msg, data); }
public void get_stats(){ m_service.get_stats(); }
}

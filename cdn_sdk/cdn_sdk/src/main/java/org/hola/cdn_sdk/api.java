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
import android.widget.VideoView;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer.upstream.HttpDataSource;
import com.google.android.exoplayer.upstream.TransferListener;
@SuppressWarnings("unused")
public class api {
final static String TAG = "HolaCDN";
public final static int MSG_SERVICE_CONNECTED = 1;
public final static int MSG_WEBSOCKET_CONNECTED = 2;
public final static int MSG_TIMEUPDATE = 3;
public final static int MSG_HOLA_LOADED = 4;
private static ServiceConnection m_conn = null;
private static service m_service;
private static Context m_ctx;
private static Bundle m_extra;
private static String m_customer;
public static boolean init(Context ctx, String customer, Bundle extra,
    final Handler callback)
{
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.KITKAT)
        return false;
    Log.d(TAG, "HolaCDN API init");
    m_ctx = ctx;
    m_extra = extra;
    m_customer = customer;
    m_conn = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName componentName,
            IBinder iBinder)
        {
            Log.d(TAG, "HolaCDN API connected to service");
            m_service = ((service.hola_service_binder) iBinder).get_service();
            m_service.init(m_customer, m_extra, callback);
            callback.sendEmptyMessage(MSG_SERVICE_CONNECTED);
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName){
            Log.d(TAG, "HolaCDN API disconnected from service");
            m_service = null;
        }
    };
    m_ctx.bindService(new Intent(ctx, service.class), m_conn,
        Context.BIND_AUTO_CREATE);
    return true;
}
public static void uninit(){
    detach();
    m_ctx.unbindService(m_conn); }
public static MediaPlayer attach(MediaPlayer player){
    if (m_service==null)
        return null;
    return m_service.attach(player);
}
public static VideoView attach(VideoView vview){
    if (m_service==null)
        return null;
    return m_service.attach(vview);
}
public static HttpDataSource attach(ExoPlayer player, String user_agent,
    TransferListener b_meter, String url)
{
    if (m_service==null)
        return null;
    m_service.attach(player, url);
    return new exoplayer_data_source(user_agent,
        null, b_meter, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
        DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, false);
}
public static void detach(){
    if (m_service!=null)
        m_service.detach();
}
public static boolean is_inited(){ return m_service!=null; }
public static boolean is_connected(){
    return m_service!=null && m_service.is_ws(); }
public static boolean is_attached(){
    return m_service!=null && m_service.is_attached(); }
public static void send_message(String msg, String data){
    m_service.send_message(msg, data); }
public static void get_stats(){ m_service.get_stats(); }
public static void bug_report(){ m_service.bug_report(); }
}

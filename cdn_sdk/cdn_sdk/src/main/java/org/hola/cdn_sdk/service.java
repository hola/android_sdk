package org.hola.cdn_sdk;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.VideoView;
import com.google.android.exoplayer.ExoPlayer;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerRequestImpl;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Vector;
public class service extends Service {
static final int MSG_STATE = 1;
static final int MSG_RESPONSE = 2;
static final int MSG_REMOVE = 3;
static final int MSG_ATTACHED = 5;
static final int MSG_TIMEUPDATE = 6;
static final int MSG_GETMODE = 7;
static final int MSG_DETACHED = 8;
static final int MSG_CHECK_HOLA = 9;
private boolean m_attached;
private WebView m_wv;
private boolean m_hola_connected = false;
private proxy_api m_proxy;
private js_proxy m_js_proxy;
private Handler m_handler;
private Handler m_callback;
private AsyncHttpServer m_serverws = new AsyncHttpServer();
private AsyncHttpServer m_dataproxy = new AsyncHttpServer();
final private Vector<request_t> m_pending = new Vector<>(50);
private Queue<String> m_msg_queue = new LinkedList<>();
private Uri m_master;
private Vector<level_info_t> m_levels = new Vector<>();
private boolean m_live = false;
private String m_mode;
private Set<String> m_media_urls = new HashSet<>();
private static int m_dp_socket;
private class level_info_t {
    public int m_bitrate;
    public String m_url;
    public Vector<segment_info_t> m_segments = new Vector<>();
}
private class segment_info_t {
    public float m_duration;
    public String m_url;
}
private class request_t {
    public String m_path;
    public AsyncHttpServerResponse m_response;
    public request_t(String path, AsyncHttpServerResponse res){
        m_path = path;
        m_response = res;
    }
}
private class http_request_t extends AsyncHttpClient.StringCallback
    implements HttpConnectCallback
{
    private final int m_fragid;
    private final int m_reqid;
    private final String m_url;
    private long m_bytes;
    private boolean m_parse;
    private long m_starttime;
    public http_request_t(int frag_id, int req_id, String url){
        m_fragid = frag_id;
        m_reqid = req_id;
        m_url = url;
        request_t m_pending_request = m_pending.elementAt(m_fragid);
        m_starttime = System.currentTimeMillis();
        String urlfile = Uri.parse(url).getLastPathSegment().toLowerCase();
        if (urlfile.endsWith(".mp4") || urlfile.endsWith(".ts"))
            m_parse = false;
        else if (urlfile.endsWith(".m3u8"))
            m_parse = true;
        else
            m_parse = !m_media_urls.contains(m_pending_request.m_path);
        Log.v(api.TAG, "request "+url+" "+m_pending_request.m_path+" "+
            m_parse);
        AsyncHttpClient client = AsyncHttpClient.getDefaultInstance();
        if (m_parse)
        {
            client.executeString(new AsyncHttpRequest(Uri.parse(m_url), "GET"),
                this);
        }
        else
        {
            client.execute(url, this);
            send_message("streamOpen", "\"req_id\":"+m_reqid);
        }
    }
    @Override
    public void onConnectCompleted(Exception ex, AsyncHttpResponse response){
        if (response==null)
            return;
        send_messages(ex, response);
        request_t resp;
        if ((resp = get_clean())==null)
            return;
        long req_dur = System.currentTimeMillis()-m_starttime;
        m_proxy.set_bitrate(get_url_level(resp.m_path).m_bitrate);
        m_proxy.set_bandwidth((int)(m_bytes*8000/req_dur));
        resp.m_response.proxy(response);
    }
    @Override
    public void onCompleted(Exception e, AsyncHttpResponse response,
        String s)
    {
        if (s==null)
            return;
        level_info_t curr_level = new level_info_t();
        segment_info_t curr_segment = new segment_info_t();
        request_t resp;
        if ((resp = get_clean()) == null)
            return;
        int state = get_url_state(m_url);
        switch (state)
        {
        case 1:
            curr_level.m_segments = new Vector<>();
            m_master = Uri.parse(m_url);
            break;
        case 2: curr_level = get_url_level(m_url); break;
        }
        String lines[] = s.split("\\r?\\n");
        if (!lines[0].startsWith("#EXTM3U"))
            send_perr("hls_playlist_error", "\"url\":\""+m_url+"\"");
        else
        {
            boolean endlist = false;
            boolean extinf = false;
            for (int i = 1; i < lines.length; i++)
            {
                if (lines[i].startsWith("#EXT-X-STREAM-INF"))
                {
                    int bw_pos;
                    if ((bw_pos = lines[i].indexOf("BANDWIDTH="))<0)
                    {
                        send_perr("hls_streaminfo_error", "\"url\":\""+
                            m_url+"\"");
                        continue;
                    }
                    int bw_end = lines[i].indexOf(",", bw_pos+10);
                    String bitrate = bw_end<0 ?
                        lines[i].substring(bw_pos+10) :
                        lines[i].substring(bw_pos+10, bw_end);
                    curr_level.m_bitrate = Integer.valueOf(bitrate);
                }
                else if (lines[i].startsWith("#EXTINF"))
                {
                    extinf = true;
                    // no top-level manifest, create a fake one
                    if (state==1)
                    {
                        state = 2;
                        curr_level.m_bitrate = 1;
                        curr_level.m_url = m_url;
                        m_master = Uri.parse(m_url);
                        m_levels.add(curr_level);
                    }
                    curr_segment.m_duration = Float.valueOf(lines[i]
                        .split(":|,", 3)[1]);
                }
                else if (lines[i].startsWith("#EXT-X-ENDLIST"))
                    endlist = true;
                else if (!lines[i].startsWith("#"))
                {
                    StringBuilder url_for_levels = new StringBuilder();
                    if (!lines[i].startsWith("http"))
                    {
                        url_for_levels.append(m_master.getScheme())
                            .append("://").append(m_master.getHost());
                        if (m_master.getPort()!=-1)
                            url_for_levels.append(":"+m_master.getPort());
                        if (!lines[i].startsWith("/"))
                        {
                            List<String> p_segs = m_master.getPathSegments();
                            p_segs = p_segs.subList(0, p_segs.size()-1);
                            for (String p_seg: p_segs)
                                url_for_levels.append('/').append(p_seg);
                            url_for_levels.append('/');
                        }
                    }
                    url_for_levels.append(lines[i]);
                    String media_url = url_for_levels.toString();
                    lines[i] = mangle_request(media_url);
                    switch (state)
                    {
                    case 1:
                        curr_level.m_url = media_url;
                        m_levels.add(curr_level);
                        curr_level = new level_info_t();
                        break;
                    case 2:
                        curr_segment.m_url = media_url;
                        curr_level.m_segments.add(curr_segment);
                        curr_segment = new segment_info_t();
                        m_media_urls.add(media_url);
                        break;
                    }
                }
            }
            m_live = extinf && !endlist;
        }
        resp.m_response.send(TextUtils.join("\n", lines));
    }
    private int get_url_state(String url){
        for (level_info_t li: m_levels)
        {
            if (url.endsWith(li.m_url))
                return 2;
        }
        return 1;
    }
    private level_info_t get_url_level(String url){
        for (level_info_t li: m_levels)
        {
            if (url.endsWith(li.m_url))
                return li;
            for (segment_info_t si: li.m_segments)
            {
                if (url.endsWith(si.m_url))
                    return li;
            }
        }
        return new level_info_t();
    }
    private void send_messages(Exception e, AsyncHttpResponse response){
        String prefix = "\"req_id\":" + m_reqid;
        if (e != null)
        {
            send_message("streamError", prefix);
            return;
        }
        m_bytes = Long.parseLong(response.headers().get("Content-Length"));
        send_message("streamProgress", prefix+",\"bytes\":"+m_bytes);
        send_message("streamHttpStatus", prefix+",\"status\":"+
            response.code());
        send_message("streamComplete", prefix);
    }
    private request_t get_clean(){
        request_t resp;
        if (m_pending.size() <= m_fragid
            || (resp = m_pending.elementAt(m_fragid)) == null)
        {
            return null;
        }
        m_pending.setElementAt(null, m_fragid);
        return resp;
    }
}
private JSONObject segment_to_json(level_info_t li, segment_info_t si){
    JSONObject jo = new JSONObject();
    try {
        jo.put("playlist_url", li.m_url)
        .put("bitrate", li.m_bitrate)
        .put("url", si.m_url)
        .put("duration", si.m_duration)
        .put("media_index", li.m_segments.indexOf(si));
    } catch(JSONException e){
        send_perr("seginf_json_error", "\"msg\":\""+e.getMessage()+"\""); }
    return jo;
}
private void init_metadata(){
    m_pending.clear();
    m_media_urls = new HashSet<>();
    m_msg_queue = new LinkedList<>();
    m_levels = new Vector<>();
    m_mode = null;
    m_master = null;
    m_live = false;
}
String get_segment_info(String url){
    JSONObject jo;
    for (level_info_t li: m_levels)
    {
        for (segment_info_t si: li.m_segments)
        {
            if (url.endsWith(si.m_url))
            {
                jo = segment_to_json(li, si);
                return jo.toString();
            }
        }
    }
    return "{}";
}
String get_levels(){
    JSONObject jo = new JSONObject();
    try {
        for (level_info_t li: m_levels)
        {
            if (li.m_segments.size()==0)
                continue;
            JSONObject level = new JSONObject();
            level.put("url", li.m_url).put("bitrate", li.m_bitrate);
            JSONArray ja = new JSONArray();
            for (segment_info_t si: li.m_segments)
                ja.put(segment_to_json(li, si));
            level.put("segments", ja);
        }
    } catch(JSONException e){
        send_perr("lvlinf_json_error", "\"msg\":\""+e.getMessage()+"\""); }
    return jo.toString();
}
boolean is_live_stream(){ return  m_live; }
void get_stats(){
    if (m_wv==null)
        return;
    m_wv.evaluateJavascript("javascript:hola_cdn.get_stats()",
        new ValueCallback<String>(){
            @Override
            public void onReceiveValue(String s){
                Log.d(api.TAG, s); }
        });
}
private void send_perr(String msg, String data){
    String perr = "\"id\":\""+msg+"\"";
    if (data!=null)
        perr += ","+data;
    send_message("perr", perr);
}
private class console_adapter extends WebChromeClient {
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage){
        String source = consoleMessage.sourceId();
        source = Uri.parse(source).getLastPathSegment();
        if (!consoleMessage.message().contains("hola_cdn is not defined"))
        {
            Log.i(api.TAG+"/JS", consoleMessage.messageLevel().name()+":"+
                source+":"+consoleMessage.lineNumber()+" "+
                consoleMessage.message());
        }
        return true;
    }
}
static String mangle_request(String url){
    String[] parts = TextUtils.split(url, "\\?");
    return mangle_request(Uri.parse(parts[0]))+(parts.length>1 ?
        "?"+parts[1] : "");
}
static String mangle_request(Uri uri){
    String new_url = "http://127.0.0.1:"+m_dp_socket+"/";
    String query, fragment;
    int port = uri.getPort();
    new_url += encodehex(uri.getScheme()+"://"+uri.getHost()
        +(port<0 ? "" : ":"+port))+uri.getPath();
    if ((query = uri.getQuery()) != null)
        new_url += "?"+query;
    if ((fragment = uri.getFragment()) != null)
        new_url += "#"+fragment;
    return new_url;
}
private static String encodehex(String inp){
    byte[] inp_b = inp.getBytes();
    StringBuilder sb = new StringBuilder();
    for (byte b: inp_b)
        sb.append(Integer.toHexString(b));
    return sb.toString();
}
private static String decodehex(String inp){
    StringBuilder sb = new StringBuilder();
    for (int i=0; i<inp.length(); i+=2)
        sb.append((char)Integer.parseInt(inp.substring(i, i+2), 16));
    return sb.toString();
}
private static String rebuild_url(AsyncHttpServerRequest request){
    AsyncHttpServerRequestImpl full_req = (AsyncHttpServerRequestImpl) request;
    String path = full_req.getStatusLine().split(" ")[1];
    int begin = path.indexOf("/", 1);
    return decodehex(path.substring(1, begin))+path.substring(begin);
}
@Override
public void onCreate(){
    super.onCreate();
    init_metadata();
    Log.i(api.TAG, "CDN Service is started");
    m_dataproxy.get("/.*", new HttpServerRequestCallback(){
        private int m_reqid = 1;
        @Override
        public void onRequest(AsyncHttpServerRequest req,
            AsyncHttpServerResponse res)
        {
            String url_str = rebuild_url(req);
            if (m_mode==null || m_mode.equals("n/a"))
                m_handler.sendEmptyMessage(MSG_GETMODE);
            if (m_reqid > m_pending.size()-1)
                m_pending.setSize(m_reqid+10);
            request_t in_request = new request_t(url_str, res);
            m_pending.setElementAt(in_request, m_reqid);
            if (m_mode==null || !m_mode.equals("cdn"))
            {
                new http_request_t(m_reqid++, m_js_proxy.get_new_reqid(),
                    url_str);
            }
            else
            {
                send_message("req", "\"url\":\""+url_str+"\",\"req_id\":"+
                    (m_reqid++)+",\"force\":"+m_media_urls.contains(url_str));
            }
        }
    });
    m_dataproxy.listen(m_dp_socket = find_free_port());
    Log.d(api.TAG, "Listening dataproxy at "+m_dp_socket);
    m_handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            Bundle data;
            switch (msg.what)
            {
            case MSG_STATE:
                data = msg.peekData();
                String old_state = data.getString("old");
                String new_state = data.getString("new");
                Log.d(api.TAG, "State changed: "+old_state+" to "+new_state);
                if (new_state.equals(old_state))
                    return;
                String ws_message = "\"data\":\""+new_state+'|'+old_state;
                if (new_state.equals("SEEKING"))
                    ws_message += '|' + Integer.toString(msg.arg1);
                ws_message += "\"";
                send_message("state", ws_message);
                break;
            case MSG_RESPONSE:
                data = msg.peekData();
                String url = data.getString("url");
                if (m_pending.size() <= msg.arg1)
                    return;
                if (m_pending.elementAt(msg.arg1) == null)
                {
                    Log.e(api.TAG, "unknown req_id " + msg.arg1);
                    return;
                }
                new http_request_t(msg.arg1, msg.arg2, url);
                break;
            case MSG_ATTACHED: m_attached = true; break;
            case MSG_DETACHED:
                m_attached = false;
                init_metadata();
                break;
            case MSG_TIMEUPDATE:
                int pos = m_proxy.getCurrentPosition();
                send_message("time", "\"pos\":"+pos);
                if (m_callback!=null)
                {
                    Message api_msg = new Message();
                    api_msg.what = api.MSG_TIMEUPDATE;
                    api_msg.arg1 = pos;
                    m_callback.sendMessage(api_msg);
                }
                break;
            case MSG_GETMODE:
                m_wv.evaluateJavascript("javascript:hola_cdn.get_mode()",
                    new ValueCallback<String>(){
                        @Override
                        public void onReceiveValue(String s){
                            if (s.equals("null"))
                            {
                                Message msg = m_handler
                                    .obtainMessage(MSG_GETMODE);
                                m_handler.sendMessageDelayed(msg, 100);
                                return;
                            }
                            if (m_mode==null && m_callback!=null)
                            {
                                m_callback
                                    .sendEmptyMessage(api.MSG_HOLA_LOADED);
                            }
                            m_mode = s.substring(1, s.length()-1);
                        }
                    });
                break;
            case MSG_CHECK_HOLA:
                if (m_hola_connected)
                    return;
                m_wv.evaluateJavascript("javascript:hola_cdn && typeof " +
                    "hola_cdn.android_message", new ValueCallback<String>()
                {
                    @Override
                    public void onReceiveValue(String s){
                        if (!s.equals("\"function\""))
                            return;
                        synchronized(m_msg_queue){
                            if (m_callback!=null && !m_hola_connected)
                            {
                                m_callback.sendEmptyMessage(api
                                    .MSG_WEBSOCKET_CONNECTED);
                            }
                            m_hola_connected = true;
                            if (m_msg_queue.size()>0)
                            {
                                for (String msg: m_msg_queue)
                                    send_message(msg);
                                m_msg_queue.clear();
                            }
                        }
                    }
                });
                Message new_msg = m_handler.obtainMessage(MSG_CHECK_HOLA);
                m_handler.sendMessageDelayed(new_msg, 250);
            }
        }
    };
    m_handler.sendMessageDelayed(m_handler.obtainMessage(MSG_CHECK_HOLA), 100);
}
@Override
public void onDestroy(){
    super.onDestroy();
    m_serverws.stop();
    m_dataproxy.stop();
    Log.i(api.TAG, "CDN Service is stopped");
}
@Override
public IBinder onBind(Intent intent){
    Log.d(api.TAG, "CDN Service is binded");
    return new hola_service_binder();
}
@Override
public boolean onUnbind(Intent intent){
    Log.d(api.TAG, "CDN Service is unbinded");
    return super.onUnbind(intent);
}
@Override
public void onRebind(Intent intent){
    super.onRebind(intent);
    Log.d(api.TAG, "CDN Service is rebinded");
}
class hola_service_binder extends Binder {
    public service get_service(){ return service.this; } }
void init(String customer, Bundle extra, Handler callback)
{
    WindowManager windowManager =
        (WindowManager) getSystemService(WINDOW_SERVICE);
    WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_TOAST,
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    );
    params.gravity = Gravity.TOP|Gravity.START;
    params.x = 0;
    params.y = 0;
    params.width = 0;
    params.height = 0;
    m_callback = callback;
    m_wv = new WebView(this);
    WebSettings ws = m_wv.getSettings();
    ws.setJavaScriptEnabled(true);
    ws.setSupportZoom(false);
    ws.setUserAgentString(ws.getUserAgentString()+" CDNService/"
        +BuildConfig.VERSION_NAME);
    m_wv.setVerticalScrollBarEnabled(false);
    final FrameLayout frame = new FrameLayout(this);
    frame.addView(m_wv);
    windowManager.addView(frame, params);
    String url = "http://player.h-cdn.com/webview?customer="+customer;
    if (extra != null)
    {
        for (String key : extra.keySet())
            url += "&"+key+"="+extra.getString(key);
    }
    m_js_proxy = new js_proxy(m_handler, this);
    m_wv.addJavascriptInterface(m_js_proxy, "hola_java_proxy");
    m_wv.setWebChromeClient(new console_adapter());
    m_wv.loadUrl(url);
    m_handler.sendEmptyMessage(MSG_GETMODE);
}
MediaPlayer attach(MediaPlayer source){
    if (m_wv == null)
        return null;
    m_proxy = new mplayer_proxy();
    m_proxy.init(source, m_handler);
    m_js_proxy.set_proxy(m_proxy);
    send_message("attach", null);
    return (MediaPlayer) m_proxy;
}
VideoView attach(VideoView source){
    if (m_wv == null)
        return null;
    m_proxy = new videoview_proxy(this);
    m_proxy.init(source, m_handler);
    m_js_proxy.set_proxy(m_proxy);
    send_message("attach", null);
    return (VideoView) m_proxy;
}
void attach(ExoPlayer player, String url){
    if (m_wv == null)
        return;
    m_proxy = new exoplayer_proxy(url);
    m_proxy.init(player, m_handler);
    m_js_proxy.set_proxy(m_proxy);
    send_message("attach", null);
}
void detach(){
    Message msg = new Message();
    Bundle state_data = new Bundle();
    msg.what = service.MSG_STATE;
    msg.arg1 = -1;
    state_data.putString("old", m_proxy.get_state());
    state_data.putString("new", "IDLE");
    msg.setData(state_data);
    m_handler.sendMessage(msg);
    m_handler.sendEmptyMessage(service.MSG_DETACHED);
}
static final Field find_field(Object obj, Class<?> type){
    Class<?> obj_class = obj.getClass();
    Field field = null;
    do {
        for (Field f: obj_class.getDeclaredFields())
        {
            if (f.getType().isAssignableFrom(type))
            {
                field = f;
                break;
            }
        }
    } while (field==null && (obj_class = obj_class.getSuperclass()) != null);
    return field;
}
static final Object get_field(Object obj, Class<?> type){
    Field field = find_field(obj, type);
    if (field==null)
        return null;
    try {
        field.setAccessible(true);
        return field.get(obj);
    } catch(IllegalAccessException e){ e.printStackTrace(); }
    return null;
}
void send_message(String cmd, String data){
    final String msg = "{\"cmd\":\""+cmd+"\""+(data!=null ? ","+data : "")+"}";
    m_handler.post(new Runnable() {
        @Override
        public void run(){ send_message(msg); }
    });
}
private void send_message(String msg){
    if (m_hola_connected)
    {
        m_wv.evaluateJavascript("javascript:hola_cdn.android_message('"+msg+
            "')", null);
        return;
    }
    synchronized(m_msg_queue){
        m_msg_queue.add(msg); }
}
boolean is_ws(){ return m_hola_connected; }
boolean is_attached(){ return m_attached; }
private int find_free_port(){
    try {
        ServerSocket socket = new ServerSocket(0);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    } catch(Exception e){ return 0; }
}
}

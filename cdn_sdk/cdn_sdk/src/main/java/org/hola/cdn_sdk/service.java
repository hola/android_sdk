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
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpRequest;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.callback.HttpConnectCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerRequestImpl;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
public class service extends Service {
static final int MSG_STATE = 1;
static final int MSG_RESPONSE = 2;
static final int MSG_REMOVE = 3;
static final int MSG_FRAGMENT = 4;
static final int MSG_ATTACHED = 5;
static final int MSG_TIMEUPDATE = 6;
private boolean m_attached;
private WebView m_wv;
private WebSocket m_socket;
private mplayer_proxy m_plr_proxy;
private js_proxy m_js_proxy;
private Handler m_handler;
private Handler m_callback;
private AsyncHttpServer m_serverws = new AsyncHttpServer();
private AsyncHttpServer m_dataproxy = new AsyncHttpServer();
final private Vector<request_t> m_pending = new Vector<>(50);
private Queue<String> m_msg_queue = new LinkedList<>();
private Uri m_master;
private Vector<level_info_t> m_levels = new Vector<>();
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
        m_starttime = System.currentTimeMillis();
        m_parse = Uri.parse(url).getLastPathSegment().toLowerCase()
            .endsWith("m3u8");
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
        send_messages(ex, response);
        request_t resp;
        if ((resp = get_clean())==null)
            return;
        long req_dur = System.currentTimeMillis()-m_starttime;
        m_plr_proxy.set_bitrate(get_url_level(resp.m_path).m_bitrate);
        m_plr_proxy.set_bandwidth((int)(m_bytes*8000/req_dur));
        resp.m_response.proxy(response);
    }
    @Override
    public void onCompleted(Exception e, AsyncHttpResponse response,
        String s)
    {
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
                    // no top-level manifest, create a fake one
                    if (state==1)
                    {
                        state = 2;
                        curr_level.m_bitrate = 1;
                        curr_level.m_url = m_url;
                        m_master = Uri.parse(m_url);
                        m_levels.add(curr_level);
                    }
                    curr_segment.m_duration = Float.valueOf(
                        lines[i].substring(8, lines[i].indexOf(",")));
                }
                else if (!lines[i].startsWith("#"))
                {
                    StringBuilder url_for_levels = new StringBuilder();
                    if (!lines[i].startsWith("http"))
                    {
                        Uri base_uri = state==1 ?
                            m_master : Uri.parse(curr_level.m_url);
                        url_for_levels.append(base_uri.getScheme())
                            .append("://").append(base_uri.getHost());
                        if (!lines[i].startsWith("/"))
                        {
                            List<String> p_segs = base_uri.getPathSegments();
                            p_segs = p_segs.subList(0, p_segs.size()-1);
                            for (String p_seg: p_segs)
                                url_for_levels.append('/').append(p_seg);
                            url_for_levels.append('/');
                        }
                    }
                    url_for_levels.append(lines[i]);
                    lines[i] = mangle_request(url_for_levels.toString());
                    switch (state)
                    {
                    case 1:
                        curr_level.m_url = url_for_levels.toString();
                        m_levels.add(curr_level);
                        curr_level = new level_info_t();
                        break;
                    case 2:
                        curr_segment.m_url = url_for_levels.toString();
                        curr_level.m_segments.add(curr_segment);
                        curr_segment = new segment_info_t();
                        break;
                    }
                }
            }
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
        if ((resp = m_pending.elementAt(m_fragid)) == null)
            return null;
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
        Log.i(api.TAG+"/JS", consoleMessage.messageLevel().name()+":"+
            source+":"+consoleMessage.lineNumber()+" "+
            consoleMessage.message());
        return true;
    }
}
static String mangle_request(String url){
    String[] parts = TextUtils.split(url, "\\?");
    return mangle_request(Uri.parse(parts[0]))+(parts.length>1 ?
        "?"+parts[1] : "");
}
static String mangle_request(Uri uri){
    String new_url = "http://127.0.0.1:5001/";
    String query, fragment;
    new_url += encodehex(uri.getScheme()+"://"+uri.getHost())+uri.getPath();
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
    Log.i(api.TAG, "CDN Service is started");
    m_serverws.websocket("/mp", new AsyncHttpServer.WebSocketRequestCallback(){
        @Override
        public void onConnected(final WebSocket websocket,
            AsyncHttpServerRequest request)
        {
            Log.i(api.TAG, "WebSocket connected");
            m_socket = websocket;
            if (m_callback!=null)
                m_callback.sendEmptyMessage(api.MSG_WEBSOCKET_CONNECTED);
            if (m_msg_queue.size()>0)
            {
                for (String msg: m_msg_queue)
                    m_socket.send(msg);
            }
        }
    });
    m_serverws.listen(5000);
    m_dataproxy.get("/.*", new HttpServerRequestCallback(){
        private int m_reqid = 1;
        @Override
        public void onRequest(AsyncHttpServerRequest req,
            AsyncHttpServerResponse res)
        {
            String url_str = rebuild_url(req);
            if (m_reqid > m_pending.size()-1)
                m_pending.setSize(m_reqid+10);
            request_t in_request = new request_t(url_str, res);
            m_pending.setElementAt(in_request, m_reqid);
            send_message("req", "\"url\":\""+url_str+"\",\"req_id\":"+
                (m_reqid++));
        }
    });
    m_dataproxy.listen(5001);
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
                if (m_pending.elementAt(msg.arg1) == null)
                {
                    Log.e(api.TAG, "unknown req_id " + msg.arg1);
                    return;
                }
                new http_request_t(msg.arg1, msg.arg2, url);
                break;
            case MSG_ATTACHED: m_attached = true; break;
            case MSG_TIMEUPDATE:
                int pos = m_plr_proxy.getCurrentPosition();
                send_message("time", "\"pos\":"+pos);
                if (m_callback!=null)
                {
                    Message api_msg = new Message();
                    api_msg.what = api.MSG_TIMEUPDATE;
                    api_msg.arg1 = pos;
                    m_callback.sendMessage(api_msg);
                }
                break;
            }
        }
    };
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
    m_wv.setWebChromeClient(new console_adapter());
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
    m_wv.loadUrl(url);
}
MediaPlayer attach(MediaPlayer source){
    if (m_wv == null)
        return null;
    m_plr_proxy = new mplayer_proxy();
    m_plr_proxy.init(source, m_handler);
    m_js_proxy.set_player(m_plr_proxy);
    send_message("attach", null);
    return m_plr_proxy;
}
void send_message(String cmd, String data){
    String msg = "{\"cmd\":\""+cmd+"\"";
    if (data!=null)
        msg += ","+data;
    msg += "}";
    if (m_socket!=null)
        m_socket.send(msg);
    else
        m_msg_queue.add(msg);
}
boolean is_ws(){ return m_socket!=null; }
boolean is_attached(){ return m_attached; }
}

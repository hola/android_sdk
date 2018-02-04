package com.holaspark.holaplayerdemo;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainActivity extends ListActivity {
static final String TAG = "HolaPlayerDemo";
private String m_customer_id;
private ArrayList<Article> m_articles;
private ImageLoader m_image_loader;
private RequestQueue m_request_queue;
private String m_playlist_json;
@Override
public void onCreate(Bundle saved_state){
    // XXX pavelki: restore saved state onCreate
    m_customer_id = getIntent().getStringExtra("customer_id");
    super.onCreate(saved_state);
    setContentView(R.layout.main_activity);
    m_request_queue = Volley.newRequestQueue(this);
    m_image_loader = new ImageLoader(m_request_queue, new ImageLoader.ImageCache() {
        private final LruCache<String, Bitmap> m_cache = new LruCache<>(10);
        public void putBitmap(String url, Bitmap bitmap) { m_cache.put(url, bitmap); }
        public Bitmap getBitmap(String url) { return m_cache.get(url); }
    });
    m_articles = new ArrayList<>();
    load_playlist();
}
public void load_playlist(){
    // XXX andrey: use player API
    final String mode = "new";
    String customer = m_customer_id.equals("demo_spark") ? "sparkdemo" : m_customer_id;
    String url = new Uri.Builder().scheme("http")
        .authority("player.h-cdn.com")
        .path("api/get_playlists").appendQueryParameter("customer", customer)
        .appendQueryParameter("last", mode).appendQueryParameter("vinfo", "1")
        .appendQueryParameter("hits", "6").appendQueryParameter("ext", "1").build()
        .toString();
    JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
        new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response){
                Iterator<String> keys = response.keys();
                while (keys.hasNext())
                {
                    String key = keys.next();
                    if (key.contains("_popular_") &&
                        response.optJSONArray(key).length()>0)
                    {
                        create_articles(response.optJSONArray(key));
                        return;
                    }
                }
                setResult(RESULT_FIRST_USER);
                finish();
            }
        }, new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError error){
            VolleyLog.e(TAG+" Error: "+error.getMessage());
        }
    }){
        @Override
        public Map<String, String> getHeaders() throws AuthFailureError {
            Map<String, String> params = new HashMap<>();
            params.put("Referer", "http://player.h-cdn.com/webview?customer=demo");
            return params;
        }
    };
    m_request_queue.add(req);
}
public void create_articles(JSONArray json){
    String lorem = getString(R.string.lorem_ipsum);
    try
    {
        m_playlist_json = json.toString();
        for (int i = 0; i<json.length() ; i++)
        {
            Log.d(TAG, json.getJSONObject(i).toString());
            JSONObject row = json.getJSONObject(i).getJSONObject("video_info");
            String desc = "N/A", poster = "";
            if (row.has("description"))
                desc = row.getString("description");
            else if (row.has("title"))
                desc = row.getString("title");
            if (row.has("poster"))
                poster = row.getString("poster");
            else if (row.has("video_poster"))
                poster = row.getString("video_poster");
            if (!row.has("url"))
                continue;
            String url = row.getString("url");
            try
            {
                long expires = Long.parseLong(Uri.parse(url)
                    .getQueryParameter("expires"));
                if (System.currentTimeMillis() > expires*1000)
                    continue;

            } catch (Exception ignored){}
            m_articles.add(new Article(poster, desc, lorem, url));
        }
        m_articles.add(new Article("","360 VR player", lorem,
            getString(R.string.video_url_hls_vr)));
        m_articles.add(new Article("","Live video", lorem,
            getString(R.string.video_url_live)));
        ArticleAdapter adapter = new ArticleAdapter(this, R.layout.listview_item,
            m_articles.toArray(new Article[0]));
        setListAdapter(adapter);
    } catch(JSONException e){ Log.d(TAG, "JSON exception "+e); }
}
@Override
protected void onListItemClick(ListView listview, View view, int pos, long id){
    Article article = m_articles.get(pos);
    Intent intent = new Intent(this, MainDemo.class);
    intent.putExtra("customer_id", m_customer_id);
    intent.putExtra("video_url", article.m_video_url);
    intent.putExtra("playlist_json", m_playlist_json);
    startActivity(intent);
}

class Article {
    String m_image_url;
    String m_title;
    String m_text;
    String m_video_url;
    Article(String image_url, String title, String text, String video_url){
        m_image_url = image_url;
        m_title = title;
        m_text = text;
        m_video_url = video_url;
    }
}

public class ArticleAdapter extends ArrayAdapter<Article> {
    Context m_context;
    int m_layout_resource_id;
    Article m_data[];
    ArticleAdapter(Context context, int layout_resource_id, Article[] data){
        super(context, layout_resource_id, data);
        m_layout_resource_id = layout_resource_id;
        m_context = context;
        m_data = data;
    }
    @Override
    public @NonNull View getView(int position, View row, @NonNull ViewGroup parent){
        if(row == null)
        {
            LayoutInflater inflater = ((Activity) m_context).getLayoutInflater();
            row = inflater.inflate(m_layout_resource_id, parent, false);
        }
        NetworkImageView image = row.findViewById(R.id.article_image);
        TextView title = row.findViewById(R.id.article_title);
        TextView text = row.findViewById(R.id.article_text);
        Article article = m_data[position];
        image.setDefaultImageResId(R.color.black);
        if (article.m_image_url!=null)
            image.setImageUrl(article.m_image_url, m_image_loader);
        title.setText(article.m_title);
        text.setText(article.m_text);
        return row;
    }
}
}

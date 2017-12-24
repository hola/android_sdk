package com.holaspark.holaplayer;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.util.AttributeSet;
import com.holaspark.holaplayer.internal.PlayerControlView;
import com.holaspark.holaplayer.internal.Const;
import com.holaspark.holaplayer.internal.ExoPlayerController;
import com.holaspark.holaplayer.internal.FullScreenPlayer;
import com.holaspark.holaplayer.internal.PlayerViewManager;
import com.holaspark.holaplayer.internal.VideoFrameLayout;
public class HolaPlayer extends FrameLayout implements PlayerAPI,
    GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener,
    ViewTreeObserver.OnScrollChangedListener
{
private static final int api_level = android.os.Build.VERSION.SDK_INT;
private AttributeSet m_attributes;
private Context m_context;
private TextureView m_texture_view;
private ExoPlayerController m_controller;
private PlayerState m_state;
private ViewTreeObserver m_observer;
private ViewGroup m_root;
private FrameLayout m_overlay;
private VideoFrameLayout m_content;
private PlayerControlView m_controlbar;
private FullScreenPlayer m_fullscreen;
private GestureDetector m_gesturedetector;
private float m_downdistance;
private int m_floating_width;
private PlayerViewManager m_viewmanager;
private ViewGroup.LayoutParams m_saved_overlay_params;
public HolaPlayer(Context context, AttributeSet attributes){
    super(context, attributes);
    Log.d(Const.TAG, "init");
    m_attributes = attributes;
    m_context = context;
    m_state = new PlayerState();
    setup_player();
}
private void setup_player(){
    LayoutInflater.from(m_context).inflate(R.layout.hola_player, this);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    m_content = (VideoFrameLayout)findViewById(R.id.hola_player_content);
    Display display = ((WindowManager)m_context.getSystemService(Context
        .WINDOW_SERVICE)).getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    m_floating_width = Math.min(size.x, size.y)/2;
    m_content.set_display_aspect(Math.min(size.x, size.y)/
        (float)Math.max(size.x, size.y));
    m_content.requestLayout();
    m_viewmanager = new PlayerViewManager();
    m_texture_view = new TextureView(m_context);
    m_texture_view.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    m_content.addView(m_texture_view, 0);
    m_overlay = (FrameLayout)findViewById(R.id.hola_ad_overlay);
    m_controller = new ExoPlayerController();
    m_state.m_inited = m_controller.init(m_context, m_attributes, m_overlay);
    m_controller.set_texture_view(m_texture_view);
    m_controlbar = (PlayerControlView)findViewById(R.id.hola_player_controlbar);
    m_controlbar.set_hola_player(this);
    m_controlbar.hide();
    m_controller.set_controlbar(m_controlbar);
    m_controller.add_event_listener(new EventListener());
    m_fullscreen = new FullScreenPlayer(m_context);
    m_gesturedetector = new GestureDetector(m_context, this);
    m_gesturedetector.setIsLongpressEnabled(false);
    m_gesturedetector.setOnDoubleTapListener(this);
    setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event){
            return m_gesturedetector.onTouchEvent(event); }
    });
    m_observer = getViewTreeObserver();
}
@Override
public void play(){
    Log.d(Const.TAG, "play "+m_state.m_inited);
    if (!m_state.m_inited)
        return;
    m_controller.play();
}
@Override
public void pause(){
    if (!m_state.m_inited)
        return;
    Log.d(Const.TAG, "pause");
    m_controller.pause();
}
@Override
public void seek(long position){
    if (!m_state.m_inited)
        return;
    Log.d(Const.TAG, "seek "+position);
    m_controller.seek(position);
}
@Override
public void load(String url){
    Log.d(Const.TAG, "load "+url+" "+m_state.m_inited);
    if (!m_state.m_inited)
        return;
    m_controller.load(url);
}
@Override
public void queue(PlayItem item){
    Log.d(Const.TAG, "queue "+item.get_media()+" "+item.get_ad_tag());
    if (!m_state.m_inited)
        return;
    m_controller.queue(item);
}
@Override
public void uninit(){
    Log.d(Const.TAG, "uninit");
    m_controller.uninit();
}
@Override
public void fullscreen(Boolean state){
    boolean new_state = state!=null ? state : !m_state.m_fullscreen;
    Log.d(Const.TAG, "fullscreen "+new_state);
    if (new_state==m_state.m_fullscreen)
        return;
    m_state.m_fullscreen = new_state;
    show_fullscreen(new_state);
}
private void show_fullscreen(boolean new_state){
    if (m_state.m_fullscreen)
        m_fullscreen.activate(this);
    else
        m_fullscreen.restore_player();
}
@Override
public void float_mode(Boolean state){
    if (!m_state.m_inited || m_state.m_floatmode == state)
        return;
    m_state.m_floatmode = state;
    if (state)
        m_observer.addOnScrollChangedListener(this);
    else
        m_observer.removeOnScrollChangedListener(this);
}
void floating_state(boolean state){
    if (m_state.m_floating==state)
        return;
    if (m_root==null)
    {
        m_root = (ViewGroup)getRootView();
        View.inflate(m_context, R.layout.hola_player_floating, m_root);
    }
    FrameLayout floating_player = (FrameLayout)m_root.findViewById(R.id
        .hola_player_floating);
    floating_player.setVisibility(state ? View.VISIBLE : View.GONE);
    floating_player.setLayoutParams(new FrameLayout.LayoutParams(
        m_floating_width, (int)(m_floating_width/m_controller.get_aspect()),
        Gravity.CENTER));
    ((FrameLayout)floating_player.getParent())
        .setLayoutParams(new FrameLayout.LayoutParams(m_floating_width+30,
        (int)(m_floating_width/m_controller.get_aspect())+30,
        Gravity.BOTTOM|Gravity.RIGHT));
    m_state.m_floating = state;
    if (state)
    {
        m_saved_overlay_params = m_overlay.getLayoutParams();
        m_overlay.setLayoutParams(new FrameLayout.LayoutParams(
            m_overlay.getWidth(), m_overlay.getHeight()));
        m_viewmanager.detach(m_content);
        floating_player.addView(m_content);
        floating_player.requestLayout();
        return;
    }
    m_viewmanager.restore(m_content);
    m_overlay.setLayoutParams(m_saved_overlay_params);
}
@Override
public void setVisibility(int visibility) {
    super.setVisibility(visibility);
    if (m_texture_view !=null)
        m_texture_view.setVisibility(visibility);
}
float get_distance(MotionEvent e){
    float distance_x = e.getX(1)-e.getX(0);
    float distance_y = e.getY(1)-e.getY(0);
    return (float)Math.sqrt(distance_x*distance_x+distance_y*distance_y);
}
@Override
public boolean onSingleTapConfirmed(MotionEvent e){
    if (!m_state.m_inited)
        return false;
    if (m_controlbar.isVisible())
        m_controlbar.hide();
    else
    {
        m_controlbar.setShowTimeoutMs(
            PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS);
        m_controlbar.show();
    }
    return true;
}
@Override
public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
    float distanceY)
{
    // XXX pavelki/andrey: add swipe to rewind/forward
    if (!m_state.m_inited || e2.getPointerCount()==1)
        return false;
    if (m_downdistance==0.0f)
        m_downdistance = get_distance(e2);
    else if (get_distance(e2)>m_downdistance*2.f)
        fullscreen(true);
    else if (get_distance(e2)<m_downdistance/2.f)
        fullscreen(false);
    return true;
}
@Override
public boolean onDoubleTap(MotionEvent e){ return false; }
@Override
public boolean onDoubleTapEvent(MotionEvent e){ return false; }
@Override
public boolean onDown(MotionEvent e){
    m_downdistance = 0.0f;
    return true;
}
@Override
public void onShowPress(MotionEvent e){}
@Override
public boolean onSingleTapUp(MotionEvent e){ return false; }
@Override
public void onLongPress(MotionEvent e){}
@Override
public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
    float velocityY)
{
    return false;
}
@Override
public void onScrollChanged(){
    Rect r = new Rect();
    r.left = 0;
    r.top = 0;
    r.right = m_overlay.getWidth();
    r.bottom = m_overlay.getHeight();
    getChildVisibleRect(m_overlay, r, null);
    if (r.height()<m_overlay.getHeight() && r.top>=0 && r.bottom>=0
        && !m_state.m_floating)
    {
        floating_state(true);
    }
    else if (r.height()==m_overlay.getHeight() && r.top>=0 && r.bottom>=0
        && m_state.m_floating)
    {
        floating_state(false);
    }
}
private class EventListener implements ExoPlayerController.EventListener {
    @Override
    public void on_video_aspect(){
        Log.d(Const.TAG, "video aspect");
        float aspect = m_controller.get_aspect();
        m_content.set_aspect(aspect);
        m_content.requestLayout();
    }
    @Override
    public void on_rendered_first(){ Log.d(Const.TAG, "rendered first"); }
}
}

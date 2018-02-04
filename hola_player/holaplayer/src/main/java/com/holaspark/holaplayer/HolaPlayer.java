package com.holaspark.holaplayer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.util.AttributeSet;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.holaspark.holaplayer.internal.PlayerControlView;
import com.holaspark.holaplayer.internal.Const;
import com.holaspark.holaplayer.internal.ExoPlayerController;
import com.holaspark.holaplayer.internal.FullScreenPlayer;
import com.holaspark.holaplayer.internal.PlayerState;
import com.holaspark.holaplayer.internal.PlayerViewManager;
import com.holaspark.holaplayer.internal.ThumbnailsController;
import com.holaspark.holaplayer.internal.VideoFrameLayout;
import com.holaspark.holaplayer.internal.WatchNextController;
import net.protyposis.android.spectaculum.InputSurfaceHolder;
import net.protyposis.android.spectaculum.LibraryHelper;
import net.protyposis.android.spectaculum.SpectaculumView;
import net.protyposis.android.spectaculum.effects.ImmersiveEffect;
import net.protyposis.android.spectaculum.gles.GLUtils;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class HolaPlayer extends FrameLayout implements HolaPlayerAPI,
    GestureDetector.OnGestureListener,
    ViewTreeObserver.OnScrollChangedListener
{
private Context m_context;
private View m_video_view;
private ExoPlayerController m_controller;
private PlayerState m_state;
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
private HolaPlayerConfig m_config;
private Rect m_temprect = new Rect();
private EventListener m_listener;
private Set<HolaPlayerAPI.EventListener> m_listeners;
private ThumbnailsController m_thumb_ctrl;
private WatchNextController m_watch_next_ctrl;
private ImmersiveEffect m_immersive;
private float m_panx;
private float m_pany;
private boolean m_controlbar_enabled;
public HolaPlayer(Context context, AttributeSet attrs){
    super(context, attrs);
    Log.d(Const.TAG, "init");
    m_context = context;
    m_state = new PlayerState();
    m_config = new HolaPlayerConfig();
    m_listeners = new CopyOnWriteArraySet<>();
    setBackgroundColor(getResources().getColor(R.color.black));
    if (attrs != null)
    {
        TypedArray style = m_context.getTheme().obtainStyledAttributes(attrs,
            R.styleable.HolaPlayer, 0, 0);
        m_config.m_customer = style.getString(R.styleable.HolaPlayer_customer);
        m_config.m_floatmode = style.getBoolean(
            R.styleable.HolaPlayer_float_mode, m_config.m_floatmode);
        m_config.m_float_close_on_touch = style.getBoolean(
            R.styleable.HolaPlayer_float_close_on_touch,
            m_config.m_float_close_on_touch);
        m_config.m_thumbnails = style.getBoolean(
            R.styleable.HolaPlayer_thumbnails, m_config.m_thumbnails);
        m_config.m_vrmode = style.getBoolean(R.styleable.HolaPlayer_vr_mode,
            m_config.m_vrmode);
        m_config.m_full_frame_thumbnails = style.getBoolean(
            R.styleable.HolaPlayer_full_frame_thumbnails,
            m_config.m_full_frame_thumbnails);
        m_config.m_watch_next = style.getBoolean(R.styleable.HolaPlayer_watch_next,
            m_config.m_watch_next);
        style.recycle();
    }
    setup_player();
}
private void setup_player(){
    LayoutInflater.from(m_context).inflate(R.layout.hola_player, this);
    setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
    m_listener = new EventListener();
    m_content = findViewById(R.id.hola_player_content);
    Display display = ((WindowManager)m_context.getSystemService(Context
        .WINDOW_SERVICE)).getDefaultDisplay();
    Point size = new Point();
    display.getSize(size);
    m_floating_width = Math.min(size.x, size.y)/2;
    m_content.set_aspect(Math.max(size.x, size.y)/
        (float)Math.min(size.x, size.y));
    m_content.requestLayout();
    m_viewmanager = new PlayerViewManager();
    m_overlay = findViewById(R.id.hola_ad_overlay);
    m_controller = new ExoPlayerController(this);
    m_state.m_inited = m_controller.init(m_context, m_overlay);
    m_controlbar_enabled = true;
    m_controlbar = findViewById(R.id.hola_player_controlbar);
    m_controlbar.set_hola_player(this);
    m_controlbar.show();
    m_controller.set_controlbar(m_controlbar);
    m_controller.add_event_listener(m_listener);
    m_controller.get_exoplayer().addListener(m_listener);
    m_fullscreen = new FullScreenPlayer(m_context);
    set_scroll_observer(m_config.m_floatmode);
    m_gesturedetector = new GestureDetector(m_context, this);
    m_gesturedetector.setIsLongpressEnabled(false);
    m_content.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event){
            return m_gesturedetector.onTouchEvent(event); }
    });
    if (m_config.m_thumbnails)
        init_thumbnails();
    if (m_config.m_watch_next)
        init_watch_next();
    check_create_video_view();
}
@Override
protected void onMeasure(int width_spec, int height_spec){
    super.onMeasure(width_spec, height_spec);
    int width_mode = MeasureSpec.getMode(width_spec);
    int width_size = MeasureSpec.getSize(width_spec);
    int height_mode = MeasureSpec.getMode(height_spec);
    int height_size = MeasureSpec.getSize(height_spec);
    float aspect = m_controller.get_aspect()==0 ? 16f/9f : m_controller.get_aspect();
    int video_width = m_controller.get_video_width()==0 ? 960 : m_controller.get_video_width();
    int video_height = m_controller.get_video_height()==0 ? 540 : m_controller.get_video_height();
    int max_width = width_mode==MeasureSpec.AT_MOST ? width_size: Integer.MAX_VALUE;
    int max_height = height_mode==MeasureSpec.AT_MOST ? height_size: Integer.MAX_VALUE;
    int width, height;
    if (width_mode==MeasureSpec.EXACTLY && height_mode==MeasureSpec.EXACTLY)
    {
        width = width_size;
        height = height_size;
    }
    else if (width_mode==MeasureSpec.EXACTLY)
    {
        width = width_size;
        height = Math.min(Math.round(width_size/aspect), max_height);
    }
    else if (height_mode==MeasureSpec.EXACTLY)
    {
        height = height_size;
        width = Math.min(Math.round(height_size*aspect), max_width);
    }
    else
    {
        float scale = video_width>max_width || video_height>max_height ?
            Math.min((float) max_width/video_width, (float)max_height/video_height) : 1;
        width = Math.round(scale*video_width);
        height = Math.round(scale*video_height);
    }
    super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
}
@Override
public void vr_mode(Boolean state){
    Log.d(Const.TAG, "set video vr mode "+state);
    if (m_config.m_vrmode==state)
        return;
    m_config.m_vrmode = state;
    check_create_video_view();
}
@Override
public void set_customer(String customer){
    m_config.m_customer = customer.equals("sparkdemo") ? "demo_spark" : customer;
    m_controller.set_customer(customer);
}
public void load_playlists(HolaPlayerCallback cb){
    m_controller.load_playlists(cb); }
private synchronized void check_create_video_view(){
    Log.d(Const.TAG, "check_create_video_view ad:"+m_controller.is_playing_ad());
    if (!m_state.m_inited || m_video_view!=null && ((m_config.m_vrmode &&
        !m_controller.is_playing_ad()) == m_state.m_vr_active))
    {
        return;
    }
    if (m_video_view!=null)
        m_content.removeView(m_video_view);
    if (m_config.m_vrmode &&  !m_controller.is_playing_ad())
    {
        Log.d(Const.TAG, "add 360 degree video view");
        m_panx = 0.0f;
        m_pany = 0.0f;
        SpectaculumView spectaculum = new SpectaculumView(m_context);
        spectaculum.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        m_immersive = new ImmersiveEffect();
        spectaculum.addEffect(m_immersive);
        spectaculum.selectEffect(0);
        m_content.addView(spectaculum, 0);
        m_controller.set_view(spectaculum);
        m_video_view = spectaculum;
        m_state.m_vr_active = true;
        return;
    }
    Log.d(Const.TAG, "add normal video view");
    TextureView texture_view = new TextureView(m_context);
    texture_view.setLayoutParams(new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));
    m_content.addView(texture_view, 0);
    m_video_view = texture_view;
    m_controller.set_view(texture_view);
    m_state.m_vr_active = false;
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
public boolean is_playing(){ return m_controller.is_playing(); }
@Override
public boolean is_playing_ad(){ return m_controller.is_playing_ad(); }
@Override
public boolean is_paused(){ return m_controller.is_paused(); }
@Override
public int get_playback_state(){ return m_controller.get_playback_state(); }
@Override
public void seek(long position){
    if (!m_state.m_inited)
        return;
    Log.d(Const.TAG, "seek "+position);
    m_controller.seek(position);
}
@Override
public long get_position(){
    return m_controller.get_exoplayer().getCurrentPosition(); }
@Override
public long get_duration(){ return m_controller.get_exoplayer().getDuration(); }
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
    m_controlbar.hide();
    if (m_thumb_ctrl!=null)
        m_thumb_ctrl.set_play_item(item);
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
    show_fullscreen();
}
private void show_fullscreen(){
    check_create_video_view();
    if (m_state.m_fullscreen)
        m_fullscreen.activate(this);
    else
        m_fullscreen.restore_player();
    for (HolaPlayerAPI.EventListener listener : m_listeners)
        listener.on_fullscreen_changed(m_state.m_fullscreen);
}
public FullScreenPlayer get_fullscreen(){ return m_fullscreen; }
private void set_scroll_observer(boolean on){
    ViewTreeObserver observer = getViewTreeObserver();
    if (on)
        observer.addOnScrollChangedListener(this);
    else
        observer.removeOnScrollChangedListener(this);
}
@Override
public void float_mode(Boolean state){
    if (!m_state.m_inited || m_config.m_floatmode == state)
        return;
    m_config.m_floatmode = state;
    set_scroll_observer(m_config.m_floatmode);
}
public void init_thumbnails(){
    if (m_thumb_ctrl!=null)
        return;
    m_thumb_ctrl = new ThumbnailsController(m_controller.get_exoplayer(), this);
}
public void init_watch_next(){
    if (m_watch_next_ctrl!=null)
        return;
    m_watch_next_ctrl = new WatchNextController(this, null);
}
public void set_watch_next_items(PlayListItem[] items){
    if (m_watch_next_ctrl==null)
        return;
    m_watch_next_ctrl.init(items);
}
@Override
public void add_listener(HolaPlayerAPI.EventListener listener){
    m_listeners.add(listener);
}
@Override
public void remove_listener(HolaPlayerAPI.EventListener listener){
    m_listeners.remove(listener);
}
@Override
public int get_video_width(){
    return m_controller.get_video_width();
}
@Override
public int get_video_height(){
    return m_controller.get_video_height();
}
@Override
public void set_controls_state(boolean enabled){
    m_controlbar_enabled = enabled;
    if (!enabled)
        m_controlbar.hide();
}
@Override
public boolean get_controls_state(){ return m_controlbar_enabled; }
@Override
public void set_controls_visibility(boolean visible){
    if (visible)
        m_controlbar.show();
    else
        m_controlbar.hide();
}
@Override
public boolean get_controls_visibility(){
    return m_controlbar.isVisible();
}
@Override
public boolean get_vr_mode(){ return m_state.m_vr_active; }
public HolaPlayerConfig get_config() { return m_config; }
private void float_state(boolean state){
    if (m_state.m_floating==state)
        return;
    if (m_controlbar.isVisible())
        m_controlbar.hide();
    if (m_root==null)
    {
        m_root = (ViewGroup)getRootView();
        View.inflate(m_context, R.layout.hola_player_floating, m_root);
    }
    m_state.m_floating = state;
    View content = m_root.findViewById(android.R.id.content);
    int pos[] = new int[2];
    content.getLocationInWindow(pos);
    int margin_right = m_root.getWidth()-content.getWidth()-pos[0]+30;
    int margin_bottom = m_root.getHeight()-content.getHeight()-pos[1]+30;
    int width = m_floating_width;
    int height = (int)(m_floating_width/m_controller.get_aspect());
    FrameLayout floating_player = m_root.findViewById(
        R.id.hola_player_floating);
    floating_player.setVisibility(m_state.m_floating ? View.VISIBLE :
        View.GONE);
    floating_player.setLayoutParams(new FrameLayout.LayoutParams(width,
        height, Gravity.TOP|Gravity.START));
    ((FrameLayout)floating_player.getParent()).setLayoutParams(
        new FrameLayout.LayoutParams(width+margin_right, height+margin_bottom,
        Gravity.BOTTOM|Gravity.END));
    check_create_video_view();
    if (m_state.m_floating)
    {
        m_saved_overlay_params = m_overlay.getLayoutParams();
        m_overlay.setLayoutParams(new FrameLayout.LayoutParams(
            m_overlay.getWidth(), m_overlay.getHeight()));
        m_viewmanager.detach(m_content);
        floating_player.addView(m_content);
        floating_player.requestLayout();
        m_controller.send_spark_event("{id: 'persistent', event: 'start'}");
        return;
    }
    m_viewmanager.restore(m_content);
    m_overlay.setLayoutParams(m_saved_overlay_params);
}
@Override
public void setVisibility(int visibility) {
    super.setVisibility(visibility);
    if (m_video_view !=null)
        m_video_view.setVisibility(visibility);
}
private float get_distance(MotionEvent e){
    float distance_x = e.getX(1)-e.getX(0);
    float distance_y = e.getY(1)-e.getY(0);
    return (float)Math.sqrt(distance_x*distance_x+distance_y*distance_y);
}
@Override
public boolean onScroll(MotionEvent e1, MotionEvent e2, float dist_x,
    float dist_y)
{
    // XXX pavelki/andrey: add swipe to rewind/forward
    Log.d(Const.TAG, "on scroll "+e1+" "+e2);
    int n_taps = e2.getPointerCount();
    if (!m_state.m_inited || n_taps==1 && !m_state.m_vr_active)
        return false;
    if (n_taps==1)
    {
        float[] matrix = new float[16];
        m_panx += dist_x*180f/m_video_view.getWidth();
        m_pany += dist_y*180f/m_video_view.getHeight();
        m_pany = LibraryHelper.clamp(m_pany, -90, 90);
        GLUtils.Matrix.setRotateEulerM(matrix, 0, -m_pany, -m_panx, 0);
        m_immersive.setRotationMatrix(matrix);
        return true;
    }
    if (m_downdistance==0.0f)
        m_downdistance = get_distance(e2);
    else if (get_distance(e2)>m_downdistance*2.f)
        fullscreen(true);
    else if (get_distance(e2)<m_downdistance/2.f)
        fullscreen(false);
    return true;
}
@Override
public boolean onDown(MotionEvent e){
    if (!m_state.m_inited)
        return false;
    m_downdistance = 0.0f;
    if (m_state.m_vr_active)
        requestDisallowInterceptTouchEvent(true);
    return true;
}
@Override
public void onShowPress(MotionEvent e){}
@Override
public boolean onSingleTapUp(MotionEvent e){
    if (!m_state.m_inited)
        return false;
    requestDisallowInterceptTouchEvent(false);
    if (m_state.m_floating)
    {
        if (m_controller.is_playing_ad())
            return true;
        if (m_config.m_float_close_on_touch)
        {
            float_state(false);
            float_mode(false);
            return true;
        }
        if (m_controller.is_paused())
            m_controller.play();
        else
            m_controller.pause();
        return true;
    }
    if (m_controlbar.isVisible())
        m_controlbar.hide();
    else if (m_controlbar_enabled)
    {
        m_controlbar.setShowTimeoutMs(
            PlayerControlView.DEFAULT_SHOW_TIMEOUT_MS);
        m_controlbar.show();
    }
    return true;
}
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
    m_temprect.set(0, 0, m_overlay.getWidth(), m_overlay.getHeight());
    getChildVisibleRect(m_overlay, m_temprect, null);
    if (m_temprect.height()<m_overlay.getHeight()/2 && !m_state.m_floating)
        float_state(true);
    else if (m_temprect.height()>m_overlay.getHeight()/2 && m_temprect.top>=0
        && m_temprect.bottom>=0 && m_state.m_floating)
    {
        float_state(false);
    }
}
private class EventListener extends Player.DefaultEventListener implements
    ExoPlayerController.VideoEventListener, InputSurfaceHolder.Callback
{
    private boolean m_play_when_ready = false;
    private int m_playback_state = Player.STATE_IDLE;
    @Override
    public void onPlayerStateChanged(boolean play_when_ready, int playback_state){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
        {
            if (m_play_when_ready!=play_when_ready)
            {
                if (play_when_ready)
                    listener.on_play();
                else
                    listener.on_pause();
            }
            if (m_playback_state!=playback_state)
                listener.on_state_changed(playback_state);
        }
        m_play_when_ready = play_when_ready;
        m_playback_state = playback_state;
    }
    @Override
    public void onSeekProcessed(){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_seeked();
    }
    @Override
    public void onPlayerError(ExoPlaybackException error){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_error(error);
    }
    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_timeline_changed(timeline, manifest);
    }
    @Override
    public void onTracksChanged(TrackGroupArray track_groups, TrackSelectionArray track_selections){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_tracks_changed(track_groups, track_selections);
    }
    @Override
    public void onLoadingChanged(boolean is_loading){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_loading_changed(is_loading);
    }
    @Override
    public void onPositionDiscontinuity(@Player.DiscontinuityReason int reason){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_position_discontinuity(reason);
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playback_parameters){
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_playback_parameters_changed(playback_parameters);
    }
    @Override
    public void on_video_size(int width, int height){
        float aspect = ((float)width)/height;
        Log.d(Const.TAG, "video aspect "+aspect);
        m_content.set_aspect(aspect);
        if (m_video_view instanceof SpectaculumView)
            ((SpectaculumView) m_video_view).updateResolution(width, height);
        m_content.requestLayout();
    }
    @Override
    public void on_rendered_first(){
        Log.d(Const.TAG, "rendered first frame"); }
    @Override
    public void on_ad_start(){
        Log.d(Const.TAG, "ad started st:"+m_controller.is_playing_ad());
        m_controlbar.hide();
        check_create_video_view();
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_ad_start();
    }
    @Override
    public void on_ad_end(){
        Log.d(Const.TAG, "ad ended st:"+m_controller.is_playing_ad());
        check_create_video_view();
        for (HolaPlayerAPI.EventListener listener : m_listeners)
            listener.on_ad_end();
    }
    @Override
    public void surfaceCreated(InputSurfaceHolder holder){
        m_controller.set_texture(holder.getSurfaceTexture()); }
    @Override
    public void surfaceDestroyed(InputSurfaceHolder holder){
        m_controller.set_texture(null); }
}
}

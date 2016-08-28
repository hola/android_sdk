package org.hola.cdn_sdk;
import android.animation.StateListAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Display;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewOverlay;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.view.ViewStructure;
import android.view.ViewTreeObserver;
import android.view.WindowId;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.MediaController;
import android.widget.VideoView;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
public class videoview_proxy extends VideoView implements proxy_api {
private VideoView m_vview;
private Handler m_handler;
private String m_state = "NONE";
private boolean m_prepared = false;
private String m_videourl;
private MediaPlayer.OnPreparedListener m_prepared_listener = null;
private MediaPlayer.OnCompletionListener m_completion_listener = null;
private MediaPlayer.OnSeekCompleteListener m_seek_listener = null;
private int m_bitrate;
private int m_bandwidth;
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
public void init(Object source, Handler hnd){
    m_vview = (VideoView) source;
    m_handler = hnd;
    update_state(m_vview.isPlaying() ? "PLAYING" : "IDLE");
    m_vview.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){
        @Override
        public void onPrepared(MediaPlayer m_player){
            videoview_proxy.this.m_prepared = true;
            videoview_proxy.this.update_state("STARTING");
            MediaPlayer internal_mp = (MediaPlayer) service.get_field(m_vview,
                MediaPlayer.class);
            internal_mp.setOnSeekCompleteListener(
            new MediaPlayer.OnSeekCompleteListener(){
                @Override
                public void onSeekComplete(MediaPlayer player){
                    videoview_proxy.this.update_state("SEEKED");
                    if (m_seek_listener!=null)
                        m_seek_listener.onSeekComplete(player);
                }
            });
            if (m_prepared_listener!=null)
                m_prepared_listener.onPrepared(m_player);
        }
    });
    m_vview.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
        @Override
        public void onCompletion(MediaPlayer m_player){
            if (!videoview_proxy.this.m_prepared)
                return;
            videoview_proxy.this.update_state("IDLE");
            if (m_completion_listener!=null)
                m_completion_listener.onCompletion(m_player);
        }
    });
}
public String get_state(){ return m_state; }
public String get_url(){ return m_videourl; }
public boolean is_prepared(){ return m_prepared; }
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
public int get_bitrate(){ return m_bitrate; }
public void set_bitrate(int br){ m_bitrate = br; }
public int get_bandwidth(){ return m_bandwidth; }
public void set_bandwidth(int br){ m_bandwidth = br; }
public String get_player_name(){ return "VideoView"; }
public videoview_proxy(Context context){ super(context); }
public videoview_proxy(Context context, AttributeSet attrs){
    super(context, attrs); }
@Override
public void addSubtitleSource(InputStream is, MediaFormat format){
    m_vview.addSubtitleSource(is, format); }
@Override
public boolean canPause(){ return m_vview.canPause(); }
@Override
public boolean canSeekBackward(){ return m_vview.canSeekBackward(); }
@Override
public boolean canSeekForward(){ return m_vview.canSeekForward(); }
@Override
public void draw(Canvas canvas){ m_vview.draw(canvas); }
@Override
public CharSequence getAccessibilityClassName(){
    return m_vview.getAccessibilityClassName(); }
@Override
public int getAudioSessionId(){ return m_vview.getAudioSessionId(); }
@Override
public int getBufferPercentage(){ return m_vview.getBufferPercentage(); }
@Override
public int getCurrentPosition(){
    return m_vview==null ? 0 : m_vview.getCurrentPosition(); }
@Override
public int getDuration(){ return m_vview.getDuration(); }
@Override
public boolean isPlaying(){ return m_vview.isPlaying(); }
@Override
public boolean onKeyDown(int keyCode, KeyEvent event){
    return m_vview.onKeyDown(keyCode, event); }
@Override
public boolean onTouchEvent(MotionEvent ev){ return m_vview.onTouchEvent(ev); }
@Override
public boolean onTrackballEvent(MotionEvent ev){
    return m_vview.onTrackballEvent(ev); }
@Override
public void pause(){
    m_vview.pause();
    update_state("PAUSED");
}
@Override
public int resolveAdjustedSize(int desiredSize, int measureSpec){
    return m_vview.resolveAdjustedSize(desiredSize, measureSpec); }
@Override
public void resume(){ m_vview.resume(); }
@Override
public void seekTo(int msec){
    update_state("SEEKING", msec);
    m_vview.seekTo(msec);
}
@Override
public void setMediaController(MediaController controller){
    m_vview.setMediaController(controller); }
@Override
public void setOnCompletionListener(MediaPlayer.OnCompletionListener l){
    m_completion_listener = l; }
@Override
public void setOnErrorListener(MediaPlayer.OnErrorListener l){
    m_vview.setOnErrorListener(l); }
@Override
public void setOnInfoListener(MediaPlayer.OnInfoListener l){
    m_vview.setOnInfoListener(l); }
@Override
public void setOnPreparedListener(MediaPlayer.OnPreparedListener l){
    m_prepared_listener = l; }
@Override
public void setVideoPath(String path){
    m_videourl = path;
    m_vview.setVideoPath(service.mangle_request(path));
}
@Override
public void setVideoURI(Uri uri){
    m_videourl = uri.toString();
    m_vview.setVideoURI(Uri.parse(service.mangle_request(uri)));
}
@Override
public void setVideoURI(Uri uri, Map<String, String> headers){
    m_videourl = uri.toString();
    m_vview.setVideoURI(Uri.parse(service.mangle_request(uri)), headers);
}
@Override
public void start(){
    m_vview.start();
    update_state("PLAYING");
}
@Override
public void stopPlayback(){
    m_vview.stopPlayback();
    update_state("IDLE");
}
@Override
public void suspend(){ m_vview.suspend(); }
@Override
public boolean gatherTransparentRegion(Region region){
    return m_vview.gatherTransparentRegion(region); }
@Override
public SurfaceHolder getHolder(){
    return m_vview==null ? new SurfaceHolder(){
        @Override
        public void addCallback(Callback callback){}
        @Override
        public void removeCallback(Callback callback){}
        @Override
        public boolean isCreating(){ return false; }
        @Override
        public void setType(int i){}
        @Override
        public void setFixedSize(int i, int i1){}
        @Override
        public void setSizeFromLayout(){}
        @Override
        public void setFormat(int i){}
        @Override
        public void setKeepScreenOn(boolean b){}
        @Override
        public Canvas lockCanvas(){ return null; }
        @Override
        public Canvas lockCanvas(Rect rect){ return null; }
        @Override
        public void unlockCanvasAndPost(Canvas canvas){}
        @Override
        public Rect getSurfaceFrame(){ return null; }
        @Override
        public Surface getSurface(){ return null; }
    } : m_vview.getHolder();
}
@Override
public void setSecure(boolean isSecure){ m_vview.setSecure(isSecure); }
@Override
public void setVisibility(int visibility){ m_vview.setVisibility(visibility); }
@Override
public void setZOrderMediaOverlay(boolean isMediaOverlay){
    m_vview.setZOrderMediaOverlay(isMediaOverlay); }
@Override
public void setZOrderOnTop(boolean onTop){
    m_vview.setZOrderOnTop(onTop); }
@Override
public void addChildrenForAccessibility(ArrayList<View> outChildren){
    m_vview.addChildrenForAccessibility(outChildren); }
@Override
public void addFocusables(ArrayList<View> views, int direction){
    m_vview.addFocusables(views, direction); }
@Override
public void addFocusables(ArrayList<View> views, int direction,
    int focusableMode)
{
    m_vview.addFocusables(views, direction, focusableMode);
}
@Override
public void addOnAttachStateChangeListener(
    OnAttachStateChangeListener listener)
{
    m_vview.addOnAttachStateChangeListener(listener);
}
@Override
public void addOnLayoutChangeListener(OnLayoutChangeListener listener){
    m_vview.addOnLayoutChangeListener(listener); }
@Override
public void addTouchables(ArrayList<View> views){
    m_vview.addTouchables(views); }
@Override
public ViewPropertyAnimator animate(){ return m_vview.animate(); }
@Override
public void announceForAccessibility(CharSequence text){
    m_vview.announceForAccessibility(text); }
@Override
public void bringToFront(){ m_vview.bringToFront(); }
@Override
public void buildDrawingCache(){ m_vview.buildDrawingCache(); }
@Override
public void buildDrawingCache(boolean autoScale){
    m_vview.buildDrawingCache(autoScale); }
@Override
public void buildLayer(){ m_vview.buildLayer(); }
@Override
public boolean callOnClick(){ return m_vview.callOnClick(); }
@Override
public void cancelLongPress(){ m_vview.cancelLongPress(); }
@Override
public boolean canResolveLayoutDirection(){
    return m_vview.canResolveLayoutDirection(); }
@Override
public boolean canResolveTextAlignment(){
    return m_vview.canResolveTextAlignment(); }
@Override
public boolean canResolveTextDirection(){
    return m_vview.canResolveTextDirection(); }
@Override
public boolean canScrollHorizontally(int direction){
    return m_vview.canScrollHorizontally(direction); }
@Override
public boolean canScrollVertically(int direction){
    return m_vview.canScrollVertically(direction); }
@Override
public boolean checkInputConnectionProxy(View view){
    return m_vview.checkInputConnectionProxy(view); }
@Override
public void clearAnimation(){ m_vview.clearAnimation(); }
@Override
public void clearFocus(){ m_vview.clearFocus(); }
public static int combineMeasuredStates(int curState, int newState){
    return View.combineMeasuredStates(curState, newState); }
@Override
public void computeScroll(){ m_vview.computeScroll(); }
@Override
public WindowInsets computeSystemWindowInsets(WindowInsets in,
    Rect outLocalInsets)
{
    return m_vview.computeSystemWindowInsets(in, outLocalInsets);
}
@Override
public AccessibilityNodeInfo createAccessibilityNodeInfo(){
    return m_vview.createAccessibilityNodeInfo(); }
@Override
public void createContextMenu(ContextMenu menu){
    m_vview.createContextMenu(menu); }
@Override
public void destroyDrawingCache(){ m_vview.destroyDrawingCache(); }
@Override
public WindowInsets dispatchApplyWindowInsets(WindowInsets insets){
    return m_vview.dispatchApplyWindowInsets(insets); }
@Override
public void dispatchConfigurationChanged(Configuration newConfig){
    m_vview.dispatchConfigurationChanged(newConfig); }
@Override
public void dispatchDisplayHint(int hint){
    m_vview.dispatchDisplayHint(hint); }
@Override
public boolean dispatchDragEvent(DragEvent event){
    return m_vview.dispatchDragEvent(event); }
@Override
public void dispatchDrawableHotspotChanged(float x, float y){
    m_vview.dispatchDrawableHotspotChanged(x, y); }
@Override
public boolean dispatchGenericMotionEvent(MotionEvent event){
    return m_vview.dispatchGenericMotionEvent(event); }
@Override
public boolean dispatchKeyEvent(KeyEvent event){
    return m_vview.dispatchKeyEvent(event); }
@Override
public boolean dispatchKeyEventPreIme(KeyEvent event){
    return m_vview.dispatchKeyEventPreIme(event); }
@Override
public boolean dispatchKeyShortcutEvent(KeyEvent event){
    return m_vview.dispatchKeyShortcutEvent(event); }
@Override
public boolean dispatchNestedFling(float velocityX, float velocityY,
    boolean consumed)
{
    return m_vview.dispatchNestedFling(velocityX, velocityY, consumed);
}
@Override
public boolean dispatchNestedPreFling(float velocityX, float velocityY){
    return m_vview.dispatchNestedPreFling(velocityX, velocityY); }
@Override
public boolean dispatchNestedPrePerformAccessibilityAction(int action,
    Bundle arguments)
{
    return m_vview
        .dispatchNestedPrePerformAccessibilityAction(action, arguments);
}
@Override
public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed,
    int[] offsetInWindow)
{
    return m_vview.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
}
@Override
public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
    int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow)
{
    return m_vview
        .dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed,
            dyUnconsumed, offsetInWindow);
}
@Override
public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event){
    return m_vview.dispatchPopulateAccessibilityEvent(event); }
@Override
public void dispatchProvideStructure(ViewStructure structure){
    m_vview.dispatchProvideStructure(structure); }
@Override
public void dispatchSystemUiVisibilityChanged(int visibility){
    m_vview.dispatchSystemUiVisibilityChanged(visibility); }
@Override
public boolean dispatchTouchEvent(MotionEvent event){
    return m_vview.dispatchTouchEvent(event); }
@Override
public boolean dispatchTrackballEvent(MotionEvent event){
    return m_vview.dispatchTrackballEvent(event); }
@Override
public boolean dispatchUnhandledMove(View focused, int direction){
    return m_vview.dispatchUnhandledMove(focused, direction); }
@Override
public void dispatchWindowFocusChanged(boolean hasFocus){
    m_vview.dispatchWindowFocusChanged(hasFocus); }
@Override
public void dispatchWindowSystemUiVisiblityChanged(int visible){
    m_vview.dispatchWindowSystemUiVisiblityChanged(visible); }
@Override
public void dispatchWindowVisibilityChanged(int visibility){
    m_vview.dispatchWindowVisibilityChanged(visibility); }
@Override
public void drawableHotspotChanged(float x, float y){
    m_vview.drawableHotspotChanged(x, y); }
@Override
public View findFocus(){ return m_vview.findFocus(); }
@Override
public void findViewsWithText(ArrayList<View> outViews,
    CharSequence searched, int flags)
{
    m_vview.findViewsWithText(outViews, searched, flags);
}
@Override
public View focusSearch(int direction){
    return m_vview.focusSearch(direction); }
@Override
public void forceLayout(){ m_vview.forceLayout(); }
public static int generateViewId(){ return View.generateViewId(); }
@Override
public int getAccessibilityLiveRegion(){
    return m_vview.getAccessibilityLiveRegion(); }
@Override
public AccessibilityNodeProvider getAccessibilityNodeProvider(){
    return m_vview.getAccessibilityNodeProvider(); }
@Override
public int getAccessibilityTraversalAfter(){
    return m_vview.getAccessibilityTraversalAfter(); }
@Override
public int getAccessibilityTraversalBefore(){
    return m_vview.getAccessibilityTraversalBefore(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getAlpha(){ return m_vview.getAlpha(); }
@Override
public Animation getAnimation(){ return m_vview.getAnimation(); }
@Override
public IBinder getApplicationWindowToken(){
    return m_vview.getApplicationWindowToken(); }
@Override
public Drawable getBackground(){ return m_vview.getBackground(); }
@Override
public ColorStateList getBackgroundTintList(){
    return m_vview.getBackgroundTintList(); }
@Override
public PorterDuff.Mode getBackgroundTintMode(){
    return m_vview.getBackgroundTintMode(); }
@Override
@ViewDebug.ExportedProperty(category = "layout")
public int getBaseline(){ return m_vview.getBaseline(); }
@Override
public float getCameraDistance(){ return m_vview.getCameraDistance(); }
@Override
public Rect getClipBounds(){ return m_vview.getClipBounds(); }
@Override
public boolean getClipBounds(Rect outRect){
    return m_vview.getClipBounds(outRect); }
@Override
@ViewDebug.ExportedProperty(category = "accessibility")
public CharSequence getContentDescription(){
    return m_vview.getContentDescription(); }
public static int getDefaultSize(int size, int measureSpec){
    return View.getDefaultSize(size, measureSpec); }
@Override
public Display getDisplay(){ return m_vview.getDisplay(); }
@Override
public Bitmap getDrawingCache(){ return m_vview.getDrawingCache(); }
@Override
public Bitmap getDrawingCache(boolean autoScale){
    return m_vview.getDrawingCache(autoScale); }
@Override
public int getDrawingCacheBackgroundColor(){
    return m_vview.getDrawingCacheBackgroundColor(); }
@Override
public int getDrawingCacheQuality(){
    return m_vview.getDrawingCacheQuality(); }
@Override
public void getDrawingRect(Rect outRect){ m_vview.getDrawingRect(outRect); }
@Override
public long getDrawingTime(){ return m_vview.getDrawingTime(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getElevation(){ return m_vview.getElevation(); }
@Override
@ViewDebug.ExportedProperty
public boolean getFilterTouchesWhenObscured(){
    return m_vview.getFilterTouchesWhenObscured(); }
@Override
@ViewDebug.ExportedProperty
public boolean getFitsSystemWindows(){
    return m_vview.getFitsSystemWindows(); }
@Override
public ArrayList<View> getFocusables(int direction){
    return m_vview.getFocusables(direction); }
@Override
public void getFocusedRect(Rect r){ m_vview.getFocusedRect(r); }
@Override
public Drawable getForeground(){ return m_vview.getForeground(); }
@Override
public int getForegroundGravity(){ return m_vview.getForegroundGravity(); }
@Override
public ColorStateList getForegroundTintList(){
    return m_vview.getForegroundTintList(); }
@Override
public PorterDuff.Mode getForegroundTintMode(){
    return m_vview.getForegroundTintMode(); }
@Override
public boolean getGlobalVisibleRect(Rect r, Point globalOffset){
    return m_vview.getGlobalVisibleRect(r, globalOffset); }
@Override
public Handler getHandler(){ return m_vview.getHandler(); }
@Override
public void getHitRect(Rect outRect){ m_vview.getHitRect(outRect); }
@Override
public int getHorizontalFadingEdgeLength(){
    return m_vview.getHorizontalFadingEdgeLength(); }
@Override
@ViewDebug.CapturedViewProperty
public int getId(){ return m_vview.getId(); }
@Override
@ViewDebug.ExportedProperty(
    category = "accessibility",
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "auto"
        ),             @ViewDebug.IntToString(
            from = 1,
            to = "yes"
        ),             @ViewDebug.IntToString(
            from = 2,
            to = "no"
        ),             @ViewDebug.IntToString(
            from = 4,
            to = "noHideDescendants"
        )}
)
public int getImportantForAccessibility(){
    return m_vview.getImportantForAccessibility(); }
@Override
public boolean getKeepScreenOn(){ return m_vview.getKeepScreenOn(); }
@Override
public KeyEvent.DispatcherState getKeyDispatcherState(){
    return m_vview.getKeyDispatcherState(); }
@Override
@ViewDebug.ExportedProperty(category = "accessibility")
public int getLabelFor(){ return m_vview.getLabelFor(); }
@Override
public int getLayerType(){ return m_vview.getLayerType(); }
@Override
@ViewDebug.ExportedProperty(
    category = "layout",
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "RESOLVED_DIRECTION_LTR"
        ),             @ViewDebug.IntToString(
            from = 1,
            to = "RESOLVED_DIRECTION_RTL"
        )}
)
public int getLayoutDirection(){ return m_vview.getLayoutDirection(); }
@Override
@ViewDebug.ExportedProperty(deepExport = true, prefix = "layout_")
public ViewGroup.LayoutParams getLayoutParams(){
    return m_vview.getLayoutParams(); }
@Override
public void getLocationInWindow(int[] location){
    m_vview.getLocationInWindow(location); }
@Override
public void getLocationOnScreen(int[] location){
    m_vview.getLocationOnScreen(location); }
@Override
public Matrix getMatrix(){ return m_vview.getMatrix(); }
@Override
public int getMinimumHeight(){ return m_vview.getMinimumHeight(); }
@Override
public int getMinimumWidth(){ return m_vview.getMinimumWidth(); }
@Override
public int getNextFocusDownId(){ return m_vview.getNextFocusDownId(); }
@Override
public int getNextFocusForwardId(){ return m_vview.getNextFocusForwardId(); }
@Override
public int getNextFocusLeftId(){ return m_vview.getNextFocusLeftId(); }
@Override
public int getNextFocusRightId(){ return m_vview.getNextFocusRightId(); }
@Override
public int getNextFocusUpId(){ return m_vview.getNextFocusUpId(); }
@Override
public OnFocusChangeListener getOnFocusChangeListener(){
    return m_vview.getOnFocusChangeListener(); }
@Override
public ViewOutlineProvider getOutlineProvider(){
    return m_vview.getOutlineProvider(); }
@Override
public ViewOverlay getOverlay(){ return m_vview.getOverlay(); }
@Override
public int getOverScrollMode(){ return m_vview.getOverScrollMode(); }
@Override
public int getPaddingBottom(){ return m_vview.getPaddingBottom(); }
@Override
public int getPaddingEnd(){ return m_vview.getPaddingEnd(); }
@Override
public int getPaddingLeft(){ return m_vview.getPaddingLeft(); }
@Override
public int getPaddingRight(){ return m_vview.getPaddingRight(); }
@Override
public int getPaddingStart(){ return m_vview.getPaddingStart(); }
@Override
public int getPaddingTop(){ return m_vview.getPaddingTop(); }
@Override
public ViewParent getParentForAccessibility(){
    return m_vview.getParentForAccessibility(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getPivotX(){ return m_vview.getPivotX(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getPivotY(){ return m_vview.getPivotY(); }
@Override
public Resources getResources(){ return m_vview.getResources(); }
@Override
public View getRootView(){ return m_vview.getRootView(); }
@Override
public WindowInsets getRootWindowInsets(){
    return m_vview.getRootWindowInsets(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getRotation(){ return m_vview.getRotation(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getRotationX(){ return m_vview.getRotationX(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getRotationY(){ return m_vview.getRotationY(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getScaleX(){ return m_vview.getScaleX(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getScaleY(){ return m_vview.getScaleY(); }
@Override
public int getScrollBarDefaultDelayBeforeFade(){
    return m_vview.getScrollBarDefaultDelayBeforeFade(); }
@Override
public int getScrollBarFadeDuration(){
    return m_vview.getScrollBarFadeDuration(); }
@Override
public int getScrollBarSize(){
    return m_vview.getScrollBarSize();
}
@Override
@ViewDebug.ExportedProperty(
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "INSIDE_OVERLAY"
        ),             @ViewDebug.IntToString(
            from = 16777216,
            to = "INSIDE_INSET"
        ),             @ViewDebug.IntToString(
            from = 33554432,
            to = "OUTSIDE_OVERLAY"
        ),             @ViewDebug.IntToString(
            from = 50331648,
            to = "OUTSIDE_INSET"
        )}
)
public int getScrollBarStyle(){ return m_vview.getScrollBarStyle(); }
@Override
public int getScrollIndicators(){ return m_vview.getScrollIndicators(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public int getSolidColor(){ return m_vview.getSolidColor(); }
@Override
public StateListAnimator getStateListAnimator(){
    return m_vview.getStateListAnimator(); }
@Override
public int getSystemUiVisibility(){
    return m_vview.getSystemUiVisibility(); }
@Override
@ViewDebug.ExportedProperty
public Object getTag(){ return m_vview.getTag(); }
@Override
public Object getTag(int key){ return m_vview.getTag(key); }
@Override
@ViewDebug.ExportedProperty(
    category = "text",
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "INHERIT"
        ),             @ViewDebug.IntToString(
            from = 1,
            to = "GRAVITY"
        ),             @ViewDebug.IntToString(
            from = 2,
            to = "TEXT_START"
        ),             @ViewDebug.IntToString(
            from = 3,
            to = "TEXT_END"
        ),             @ViewDebug.IntToString(
            from = 4,
            to = "CENTER"
        ),             @ViewDebug.IntToString(
            from = 5,
            to = "VIEW_START"
        ),             @ViewDebug.IntToString(
            from = 6,
            to = "VIEW_END"
        )}
)
public int getTextAlignment(){ return m_vview.getTextAlignment(); }
@Override
@ViewDebug.ExportedProperty(
    category = "text",
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "INHERIT"
        ),             @ViewDebug.IntToString(
            from = 1,
            to = "FIRST_STRONG"
        ),             @ViewDebug.IntToString(
            from = 2,
            to = "ANY_RTL"
        ),             @ViewDebug.IntToString(
            from = 3,
            to = "LTR"
        ),             @ViewDebug.IntToString(
            from = 4,
            to = "RTL"
        ),             @ViewDebug.IntToString(
            from = 5,
            to = "LOCALE"
        ),             @ViewDebug.IntToString(
            from = 6,
            to = "FIRST_STRONG_LTR"
        ),             @ViewDebug.IntToString(
            from = 7,
            to = "FIRST_STRONG_RTL"
        )}
)
public int getTextDirection(){ return m_vview.getTextDirection(); }
@Override
public ArrayList<View> getTouchables(){ return m_vview.getTouchables(); }
@Override
public TouchDelegate getTouchDelegate(){ return m_vview.getTouchDelegate(); }
@Override
@ViewDebug.ExportedProperty
public String getTransitionName(){ return m_vview.getTransitionName(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getTranslationX(){ return m_vview.getTranslationX(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getTranslationY(){ return m_vview.getTranslationY(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getTranslationZ(){ return m_vview.getTranslationZ(); }
@Override
public int getVerticalFadingEdgeLength(){
    return m_vview.getVerticalFadingEdgeLength(); }
@Override
public int getVerticalScrollbarPosition(){
    return m_vview.getVerticalScrollbarPosition(); }
@Override
public int getVerticalScrollbarWidth(){
    return m_vview.getVerticalScrollbarWidth(); }
@Override
public ViewTreeObserver getViewTreeObserver(){
    return m_vview.getViewTreeObserver(); }
@Override
@ViewDebug.ExportedProperty(
    mapping = {            @ViewDebug.IntToString(
            from = 0,
            to = "VISIBLE"
        ),             @ViewDebug.IntToString(
            from = 4,
            to = "INVISIBLE"
        ),             @ViewDebug.IntToString(
            from = 8,
            to = "GONE"
        )}
)
public int getVisibility(){ return m_vview.getVisibility(); }
@Override
public WindowId getWindowId(){ return m_vview.getWindowId(); }
@Override
public int getWindowSystemUiVisibility(){
    return m_vview.getWindowSystemUiVisibility(); }
@Override
public IBinder getWindowToken(){ return m_vview.getWindowToken(); }
@Override
public int getWindowVisibility(){ return m_vview.getWindowVisibility(); }
@Override
public void getWindowVisibleDisplayFrame(Rect outRect){
    m_vview.getWindowVisibleDisplayFrame(outRect); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getX(){ return m_vview.getX(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getY(){ return m_vview.getY(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public float getZ(){ return m_vview.getZ(); }
@Override
@ViewDebug.ExportedProperty(category = "focus")
public boolean hasFocus(){ return m_vview.hasFocus(); }
@Override
public boolean hasFocusable(){ return m_vview.hasFocusable(); }
@Override
public boolean hasNestedScrollingParent(){
    return m_vview.hasNestedScrollingParent(); }
@Override
public boolean hasOnClickListeners(){ return m_vview.hasOnClickListeners(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean hasOverlappingRendering(){
    return m_vview.hasOverlappingRendering(); }
@Override
@ViewDebug.ExportedProperty(category = "layout")
public boolean hasTransientState(){ return m_vview.hasTransientState(); }
@Override
public boolean hasWindowFocus(){ return m_vview.hasWindowFocus(); }
public static View inflate(Context context, int resource, ViewGroup root){
    return View.inflate(context, resource, root); }
@Override
public void invalidate(){ m_vview.invalidate(); }
@Override
public void invalidate(Rect dirty){ m_vview.invalidate(dirty); }
@Override
public void invalidate(int l, int t, int r, int b){
    m_vview.invalidate(l, t, r, b); }
@Override
public void invalidateDrawable(Drawable drawable){
    m_vview.invalidateDrawable(drawable); }
@Override
public void invalidateOutline(){ m_vview.invalidateOutline(); }
@Override
public boolean isAccessibilityFocused(){
    return m_vview.isAccessibilityFocused(); }
@Override
@ViewDebug.ExportedProperty
public boolean isActivated(){ return m_vview.isActivated(); }
@Override
public boolean isAttachedToWindow(){ return m_vview.isAttachedToWindow(); }
@Override
@ViewDebug.ExportedProperty
public boolean isClickable(){ return m_vview.isClickable(); }
@Override
public boolean isContextClickable(){ return m_vview.isContextClickable(); }
@Override
public boolean isDirty(){ return m_vview.isDirty(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean isDrawingCacheEnabled(){
    return m_vview.isDrawingCacheEnabled(); }
@Override
public boolean isDuplicateParentStateEnabled(){
    return m_vview.isDuplicateParentStateEnabled(); }
@Override
@ViewDebug.ExportedProperty
public boolean isEnabled(){ return m_vview.isEnabled(); }
@Override
@ViewDebug.ExportedProperty(category = "focus")
public boolean isFocused(){ return m_vview.isFocused(); }
@Override
@ViewDebug.ExportedProperty
public boolean isHapticFeedbackEnabled(){
    return m_vview.isHapticFeedbackEnabled(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean isHardwareAccelerated(){
    return m_vview.isHardwareAccelerated(); }
@Override
public boolean isHorizontalFadingEdgeEnabled(){
    return m_vview.isHorizontalFadingEdgeEnabled(); }
@Override
public boolean isHorizontalScrollBarEnabled(){
    return m_vview.isHorizontalScrollBarEnabled(); }
@Override
@ViewDebug.ExportedProperty
public boolean isHovered(){ return m_vview.isHovered(); }
@Override
public boolean isImportantForAccessibility(){
    return m_vview.isImportantForAccessibility(); }
@Override
public boolean isInEditMode(){ return m_vview.isInEditMode(); }
@Override
public boolean isInLayout(){ return m_vview.isInLayout(); }
@Override
@ViewDebug.ExportedProperty
public boolean isInTouchMode(){ return m_vview.isInTouchMode(); }
@Override
public boolean isLaidOut(){ return m_vview.isLaidOut(); }
@Override
public boolean isLayoutDirectionResolved(){
    return m_vview.isLayoutDirectionResolved(); }
@Override
public boolean isLayoutRequested(){ return m_vview.isLayoutRequested(); }
@Override
public boolean isLongClickable(){ return m_vview.isLongClickable(); }
@Override
public boolean isNestedScrollingEnabled(){
    return m_vview.isNestedScrollingEnabled(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean isOpaque(){ return m_vview.isOpaque(); }
@Override
public boolean isPaddingRelative(){ return m_vview.isPaddingRelative(); }
@Override
@ViewDebug.ExportedProperty
public boolean isPressed(){ return m_vview.isPressed(); }
@Override
public boolean isSaveEnabled(){ return m_vview.isSaveEnabled(); }
@Override
public boolean isSaveFromParentEnabled(){
    return m_vview.isSaveFromParentEnabled(); }
@Override
public boolean isScrollbarFadingEnabled(){
    return m_vview.isScrollbarFadingEnabled(); }
@Override
public boolean isScrollContainer(){ return m_vview.isScrollContainer(); }
@Override
@ViewDebug.ExportedProperty
public boolean isSelected(){ return m_vview.isSelected(); }
@Override
public boolean isShown(){ return m_vview.isShown(); }
@Override
@ViewDebug.ExportedProperty
public boolean isSoundEffectsEnabled(){
    return m_vview.isSoundEffectsEnabled(); }
@Override
public boolean isTextAlignmentResolved(){
    return m_vview.isTextAlignmentResolved(); }
@Override
public boolean isTextDirectionResolved(){
    return m_vview.isTextDirectionResolved(); }
@Override
public boolean isVerticalFadingEdgeEnabled(){
    return m_vview.isVerticalFadingEdgeEnabled(); }
@Override
public boolean isVerticalScrollBarEnabled(){
    return m_vview.isVerticalScrollBarEnabled(); }
@Override
public void jumpDrawablesToCurrentState(){
    m_vview.jumpDrawablesToCurrentState(); }
@Override
public void layout(int l, int t, int r, int b){
    m_vview.layout(l, t, r, b); }
public static int[] mergeDrawableStates(int[] baseState, int[] additionalState)
{
    return View.mergeDrawableStates(baseState, additionalState);
}
@Override
public void offsetLeftAndRight(int offset){
    m_vview.offsetLeftAndRight(offset); }
@Override
public void offsetTopAndBottom(int offset){
    m_vview.offsetTopAndBottom(offset); }
@Override
public WindowInsets onApplyWindowInsets(WindowInsets insets){
    return m_vview.onApplyWindowInsets(insets); }
@Override
public void onCancelPendingInputEvents(){
    m_vview.onCancelPendingInputEvents(); }
@Override
public boolean onCheckIsTextEditor(){ return m_vview.onCheckIsTextEditor(); }
@Override
public InputConnection onCreateInputConnection(EditorInfo outAttrs){
    return m_vview.onCreateInputConnection(outAttrs); }
@Override
public boolean onDragEvent(DragEvent event){
    return m_vview.onDragEvent(event); }
@Override
public void onDrawForeground(Canvas canvas){
    m_vview.onDrawForeground(canvas); }
@Override
public boolean onFilterTouchEventForSecurity(MotionEvent event){
    return m_vview.onFilterTouchEventForSecurity(event); }
@Override
public void onFinishTemporaryDetach(){
    m_vview.onFinishTemporaryDetach(); }
@Override
public boolean onGenericMotionEvent(MotionEvent event){
    return m_vview.onGenericMotionEvent(event); }
@Override
public void onHoverChanged(boolean hovered){
    m_vview.onHoverChanged(hovered); }
@Override
public boolean onHoverEvent(MotionEvent event){
    return m_vview.onHoverEvent(event); }
@Override
public boolean onKeyLongPress(int keyCode, KeyEvent event){
    return m_vview.onKeyLongPress(keyCode, event); }
@Override
public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event){
    return m_vview.onKeyMultiple(keyCode, repeatCount, event); }
@Override
public boolean onKeyPreIme(int keyCode, KeyEvent event){
    return m_vview.onKeyPreIme(keyCode, event); }
@Override
public boolean onKeyShortcut(int keyCode, KeyEvent event){
    return m_vview.onKeyShortcut(keyCode, event); }
@Override
public boolean onKeyUp(int keyCode, KeyEvent event){
    return m_vview.onKeyUp(keyCode, event); }
@Override
public void onProvideStructure(ViewStructure structure){
    m_vview.onProvideStructure(structure); }
@Override
public void onProvideVirtualStructure(ViewStructure structure){
    m_vview.onProvideVirtualStructure(structure); }
@Override
public void onRtlPropertiesChanged(int layoutDirection){
    m_vview.onRtlPropertiesChanged(layoutDirection); }
@Override
public void onScreenStateChanged(int screenState){
    m_vview.onScreenStateChanged(screenState); }
@Override
public void onStartTemporaryDetach(){
    m_vview.onStartTemporaryDetach(); }
@Override
public void onWindowFocusChanged(boolean hasWindowFocus){
    m_vview.onWindowFocusChanged(hasWindowFocus); }
@Override
public void onWindowSystemUiVisibilityChanged(int visible){
    m_vview.onWindowSystemUiVisibilityChanged(visible); }
@Override
public boolean performAccessibilityAction(int action, Bundle arguments){
    return m_vview.performAccessibilityAction(action, arguments); }
@Override
public boolean performClick(){ return m_vview.performClick(); }
@Override
public boolean performContextClick(){ return m_vview.performContextClick(); }
@Override
public boolean performHapticFeedback(int feedbackConstant){
    return m_vview.performHapticFeedback(feedbackConstant); }
@Override
public boolean performHapticFeedback(int feedbackConstant, int flags){
    return m_vview.performHapticFeedback(feedbackConstant, flags); }
@Override
public boolean performLongClick(){ return m_vview.performLongClick(); }
@Override
public void playSoundEffect(int soundConstant){
    m_vview.playSoundEffect(soundConstant); }
@Override
public boolean post(Runnable action){ return m_vview.post(action); }
@Override
public boolean postDelayed(Runnable action, long delayMillis){
    return m_vview.postDelayed(action, delayMillis); }
@Override
public void postInvalidate(){ m_vview.postInvalidate(); }
@Override
public void postInvalidate(int left, int top, int right, int bottom){
    m_vview.postInvalidate(left, top, right, bottom); }
@Override
public void postInvalidateDelayed(long delayMilliseconds){
    m_vview.postInvalidateDelayed(delayMilliseconds); }
@Override
public void postInvalidateDelayed(long delayMilliseconds, int left, int top,
    int right, int bottom)
{
    m_vview.postInvalidateDelayed(delayMilliseconds, left, top, right, bottom);
}
@Override
public void postInvalidateOnAnimation(){
    m_vview.postInvalidateOnAnimation(); }
@Override
public void postInvalidateOnAnimation(int left, int top, int right, int bottom)
{
    m_vview.postInvalidateOnAnimation(left, top, right, bottom);
}
@Override
public void postOnAnimation(Runnable action){
    m_vview.postOnAnimation(action); }
@Override
public void postOnAnimationDelayed(Runnable action, long delayMillis){
    m_vview.postOnAnimationDelayed(action, delayMillis); }
@Override
public void refreshDrawableState(){ m_vview.refreshDrawableState(); }
@Override
public boolean removeCallbacks(Runnable action){
    return m_vview.removeCallbacks(action); }
@Override
public void removeOnAttachStateChangeListener(
    OnAttachStateChangeListener listener)
{
    m_vview.removeOnAttachStateChangeListener(listener);
}
@Override
public void removeOnLayoutChangeListener(
    OnLayoutChangeListener listener)
{
    m_vview.removeOnLayoutChangeListener(listener);
}
@Override
public void requestApplyInsets(){ m_vview.requestApplyInsets(); }
@Override
@Deprecated
public void requestFitSystemWindows(){
    m_vview.requestFitSystemWindows(); }
@Override
public boolean requestFocus(int direction, Rect previouslyFocusedRect){
    return m_vview==null ?
        true : m_vview.requestFocus(direction, previouslyFocusedRect);
}
@Override
public void requestLayout(){ m_vview.requestLayout(); }
@Override
public boolean requestRectangleOnScreen(Rect rectangle){
    return m_vview.requestRectangleOnScreen(rectangle); }
@Override
public boolean requestRectangleOnScreen(Rect rectangle, boolean immediate){
    return m_vview.requestRectangleOnScreen(rectangle, immediate); }
public static int resolveSize(int size, int measureSpec){
    return View.resolveSize(size, measureSpec); }
public static int resolveSizeAndState(int size, int measureSpec,
    int childMeasuredState)
{
    return View.resolveSizeAndState(size, measureSpec, childMeasuredState);
}
@Override
public void restoreHierarchyState(SparseArray<Parcelable> container){
    m_vview.restoreHierarchyState(container); }
@Override
public void saveHierarchyState(SparseArray<Parcelable> container){
    m_vview.saveHierarchyState(container); }
@Override
public void scheduleDrawable(Drawable who, Runnable what, long when){
    m_vview.scheduleDrawable(who, what, when); }
@Override
public void scrollBy(int x, int y){ m_vview.scrollBy(x, y); }
@Override
public void scrollTo(int x, int y){ m_vview.scrollTo(x, y); }
@Override
public void sendAccessibilityEvent(int eventType){
    m_vview.sendAccessibilityEvent(eventType); }
@Override
public void sendAccessibilityEventUnchecked(AccessibilityEvent event){
    m_vview.sendAccessibilityEventUnchecked(event); }
@Override
public void setAccessibilityDelegate(AccessibilityDelegate delegate){
    m_vview.setAccessibilityDelegate(delegate); }
@Override
public void setAccessibilityLiveRegion(int mode){
    m_vview.setAccessibilityLiveRegion(mode); }
@Override
public void setAccessibilityTraversalAfter(int afterId){
    m_vview.setAccessibilityTraversalAfter(afterId); }
@Override
public void setAccessibilityTraversalBefore(int beforeId){
    m_vview.setAccessibilityTraversalBefore(beforeId); }
@Override
public void setActivated(boolean activated){ m_vview.setActivated(activated); }
@Override
public void setAlpha(float alpha){ m_vview.setAlpha(alpha); }
@Override
public void setAnimation(Animation animation){
    m_vview.setAnimation(animation); }
@Override
public void setBackground(Drawable background){
    m_vview.setBackground(background); }
@Override
public void setBackgroundColor(int color){
    m_vview.setBackgroundColor(color); }
@Override
@Deprecated
public void setBackgroundDrawable(Drawable background){
    m_vview.setBackgroundDrawable(background); }
@Override
public void setBackgroundResource(int resid){
    m_vview.setBackgroundResource(resid); }
@Override
public void setBackgroundTintList(ColorStateList tint){
    m_vview.setBackgroundTintList(tint); }
@Override
public void setBackgroundTintMode(PorterDuff.Mode tintMode){
    m_vview.setBackgroundTintMode(tintMode); }
@Override
public void setCameraDistance(float distance){
    m_vview.setCameraDistance(distance); }
@Override
public void setClickable(boolean clickable){
    m_vview.setClickable(clickable); }
@Override
public void setClipBounds(Rect clipBounds){
    m_vview.setClipBounds(clipBounds); }
@Override
public void setClipToOutline(boolean clipToOutline){
    m_vview.setClipToOutline(clipToOutline); }
@Override
public void setContentDescription(CharSequence contentDescription){
    m_vview.setContentDescription(contentDescription); }
@Override
public void setContextClickable(boolean contextClickable){
    m_vview.setContextClickable(contextClickable); }
@Override
public void setDrawingCacheBackgroundColor(int color){
    m_vview.setDrawingCacheBackgroundColor(color); }
@Override
public void setDrawingCacheEnabled(boolean enabled){
    m_vview.setDrawingCacheEnabled(enabled); }
@Override
public void setDrawingCacheQuality(int quality){
    m_vview.setDrawingCacheQuality(quality); }
@Override
public void setDuplicateParentStateEnabled(boolean enabled){
    m_vview.setDuplicateParentStateEnabled(enabled); }
@Override
public void setElevation(float elevation){
    m_vview.setElevation(elevation); }
@Override
public void setEnabled(boolean enabled){
    m_vview.setEnabled(enabled); }
@Override
public void setFadingEdgeLength(int length){
    m_vview.setFadingEdgeLength(length); }
@Override
public void setFilterTouchesWhenObscured(boolean enabled){
    m_vview.setFilterTouchesWhenObscured(enabled); }
@Override
public void setFitsSystemWindows(boolean fitSystemWindows){
    m_vview.setFitsSystemWindows(fitSystemWindows); }
@Override
public void setFocusable(boolean focusable){
    if (m_vview!=null)
        m_vview.setFocusable(focusable);
}
@Override
public void setFocusableInTouchMode(boolean focusableInTouchMode){
    if (m_vview!=null)
        m_vview.setFocusableInTouchMode(focusableInTouchMode);
}
@Override
public void setForeground(Drawable foreground){
    m_vview.setForeground(foreground); }
@Override
public void setForegroundGravity(int gravity){
    m_vview.setForegroundGravity(gravity); }
@Override
public void setForegroundTintList(ColorStateList tint){
    m_vview.setForegroundTintList(tint); }
@Override
public void setForegroundTintMode(PorterDuff.Mode tintMode){
    m_vview.setForegroundTintMode(tintMode); }
@Override
public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled){
    m_vview.setHapticFeedbackEnabled(hapticFeedbackEnabled); }
@Override
public void setHasTransientState(boolean hasTransientState){
    m_vview.setHasTransientState(hasTransientState); }
@Override
public void setHorizontalFadingEdgeEnabled(boolean horizontalFadingEdgeEnabled)
{
    m_vview.setHorizontalFadingEdgeEnabled(horizontalFadingEdgeEnabled);
}
@Override
public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled)
{
    m_vview.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
}
@Override
public void setHovered(boolean hovered){ m_vview.setHovered(hovered); }
@Override
public void setId(int id){ m_vview.setId(id); }
@Override
public void setImportantForAccessibility(int mode){
    m_vview.setImportantForAccessibility(mode); }
@Override
public void setKeepScreenOn(boolean keepScreenOn){
    m_vview.setKeepScreenOn(keepScreenOn); }
@Override
public void setLabelFor(int id){ m_vview.setLabelFor(id); }
@Override
public void setLayerPaint(Paint paint){ m_vview.setLayerPaint(paint); }
@Override
public void setLayerType(int layerType, Paint paint){
    m_vview.setLayerType(layerType, paint); }
@Override
public void setLayoutDirection(int layoutDirection){
    m_vview.setLayoutDirection(layoutDirection); }
@Override
public void setLayoutParams(ViewGroup.LayoutParams params){
    m_vview.setLayoutParams(params); }
@Override
public void setLongClickable(boolean longClickable){
    m_vview.setLongClickable(longClickable); }
@Override
public void setMinimumHeight(int minHeight){
    m_vview.setMinimumHeight(minHeight); }
@Override
public void setMinimumWidth(int minWidth){
    m_vview.setMinimumWidth(minWidth); }
@Override
public void setNestedScrollingEnabled(boolean enabled){
    m_vview.setNestedScrollingEnabled(enabled); }
@Override
public void setNextFocusDownId(int nextFocusDownId){
    m_vview.setNextFocusDownId(nextFocusDownId); }
@Override
public void setNextFocusForwardId(int nextFocusForwardId){
    m_vview.setNextFocusForwardId(nextFocusForwardId); }
@Override
public void setNextFocusLeftId(int nextFocusLeftId){
    m_vview.setNextFocusLeftId(nextFocusLeftId); }
@Override
public void setNextFocusRightId(int nextFocusRightId){
    m_vview.setNextFocusRightId(nextFocusRightId); }
@Override
public void setNextFocusUpId(int nextFocusUpId){
    m_vview.setNextFocusUpId(nextFocusUpId); }
@Override
public void setOnApplyWindowInsetsListener(
    OnApplyWindowInsetsListener listener)
{
    m_vview.setOnApplyWindowInsetsListener(listener);
}
@Override
public void setOnClickListener(OnClickListener l){
    m_vview.setOnClickListener(l); }
@Override
public void setOnContextClickListener(OnContextClickListener l){
    m_vview.setOnContextClickListener(l); }
@Override
public void setOnCreateContextMenuListener(OnCreateContextMenuListener l){
    m_vview.setOnCreateContextMenuListener(l); }
@Override
public void setOnDragListener(OnDragListener l){
    m_vview.setOnDragListener(l); }
@Override
public void setOnFocusChangeListener(OnFocusChangeListener l){
    m_vview.setOnFocusChangeListener(l); }
@Override
public void setOnGenericMotionListener(OnGenericMotionListener l){
    m_vview.setOnGenericMotionListener(l); }
@Override
public void setOnHoverListener(OnHoverListener l){
    m_vview.setOnHoverListener(l); }
@Override
public void setOnKeyListener(OnKeyListener l){
    m_vview.setOnKeyListener(l); }
@Override
public void setOnLongClickListener(OnLongClickListener l){
    m_vview.setOnLongClickListener(l); }
@Override
public void setOnScrollChangeListener(OnScrollChangeListener l){
    m_vview.setOnScrollChangeListener(l); }
@Override
public void setOnSystemUiVisibilityChangeListener(
    OnSystemUiVisibilityChangeListener l)
{
    m_vview.setOnSystemUiVisibilityChangeListener(l);
}
@Override
public void setOnTouchListener(OnTouchListener l){
    m_vview.setOnTouchListener(l); }
@Override
public void setOutlineProvider(ViewOutlineProvider provider){
    m_vview.setOutlineProvider(provider); }
@Override
public void setOverScrollMode(int overScrollMode){
    if (m_vview!=null)
        m_vview.setOverScrollMode(overScrollMode);
}
@Override
public void setPadding(int left, int top, int right, int bottom){
    m_vview.setPadding(left, top, right, bottom); }
@Override
public void setPaddingRelative(int start, int top, int end, int bottom){
    m_vview.setPaddingRelative(start, top, end, bottom); }
@Override
public void setPivotX(float pivotX){ m_vview.setPivotX(pivotX); }
@Override
public void setPivotY(float pivotY){ m_vview.setPivotY(pivotY); }
@Override
public void setPressed(boolean pressed){ m_vview.setPressed(pressed); }
@Override
public void setRotation(float rotation){ m_vview.setRotation(rotation); }
@Override
public void setRotationX(float rotationX){ m_vview.setRotationX(rotationX); }
@Override
public void setRotationY(float rotationY){ m_vview.setRotationY(rotationY); }
@Override
public void setSaveEnabled(boolean enabled){ m_vview.setSaveEnabled(enabled); }
@Override
public void setSaveFromParentEnabled(boolean enabled){
    m_vview.setSaveFromParentEnabled(enabled); }
@Override
public void setScaleX(float scaleX){ m_vview.setScaleX(scaleX); }
@Override
public void setScaleY(float scaleY){ m_vview.setScaleY(scaleY); }
@Override
public void setScrollBarDefaultDelayBeforeFade(
    int scrollBarDefaultDelayBeforeFade)
{
    m_vview.setScrollBarDefaultDelayBeforeFade(scrollBarDefaultDelayBeforeFade);
}
@Override
public void setScrollBarFadeDuration(int scrollBarFadeDuration){
    m_vview.setScrollBarFadeDuration(scrollBarFadeDuration); }
@Override
public void setScrollbarFadingEnabled(boolean fadeScrollbars){
    m_vview.setScrollbarFadingEnabled(fadeScrollbars); }
@Override
public void setScrollBarSize(int scrollBarSize){
    m_vview.setScrollBarSize(scrollBarSize); }
@Override
public void setScrollBarStyle(int style){ m_vview.setScrollBarStyle(style); }
@Override
public void setScrollContainer(boolean isScrollContainer){
    m_vview.setScrollContainer(isScrollContainer); }
@Override
public void setScrollIndicators(int indicators){
    m_vview.setScrollIndicators(indicators); }
@Override
public void setScrollIndicators(int indicators, int mask){
    m_vview.setScrollIndicators(indicators, mask); }
@Override
public void setScrollX(int value){ m_vview.setScrollX(value); }
@Override
public void setScrollY(int value){ m_vview.setScrollY(value); }
@Override
public void setSelected(boolean selected){ m_vview.setSelected(selected); }
@Override
public void setSoundEffectsEnabled(boolean soundEffectsEnabled){
    m_vview.setSoundEffectsEnabled(soundEffectsEnabled); }
@Override
public void setStateListAnimator(StateListAnimator stateListAnimator){
    m_vview.setStateListAnimator(stateListAnimator); }
@Override
public void setSystemUiVisibility(int visibility){
    m_vview.setSystemUiVisibility(visibility); }
@Override
public void setTag(int key, Object tag){ m_vview.setTag(key, tag); }
@Override
public void setTag(Object tag){ m_vview.setTag(tag); }
@Override
public void setTextAlignment(int textAlignment){
    m_vview.setTextAlignment(textAlignment); }
@Override
public void setTextDirection(int textDirection){
    m_vview.setTextDirection(textDirection); }
@Override
public void setTouchDelegate(TouchDelegate delegate){
    m_vview.setTouchDelegate(delegate); }
@Override
public void setTranslationX(float translationX){
    m_vview.setTranslationX(translationX); }
@Override
public void setTranslationY(float translationY){
    m_vview.setTranslationY(translationY); }
@Override
public void setTranslationZ(float translationZ){
    m_vview.setTranslationZ(translationZ); }
@Override
public void setVerticalFadingEdgeEnabled(boolean verticalFadingEdgeEnabled){
    m_vview.setVerticalFadingEdgeEnabled(verticalFadingEdgeEnabled); }
@Override
public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled){
    m_vview.setVerticalScrollBarEnabled(verticalScrollBarEnabled); }
@Override
public void setVerticalScrollbarPosition(int position){
    m_vview.setVerticalScrollbarPosition(position); }
@Override
public void setWillNotCacheDrawing(boolean willNotCacheDrawing){
    m_vview.setWillNotCacheDrawing(willNotCacheDrawing); }
@Override
public void setWillNotDraw(boolean willNotDraw){
    if (m_vview!=null)
        m_vview.setWillNotDraw(willNotDraw);
}
@Override
public void setX(float x){ m_vview.setX(x); }
@Override
public void setY(float y){ m_vview.setY(y); }
@Override
public void setZ(float z){ m_vview.setZ(z); }
@Override
public boolean showContextMenu(){ return m_vview.showContextMenu(); }
@Override
public ActionMode startActionMode(ActionMode.Callback callback){
    return m_vview.startActionMode(callback);
}
@Override
public ActionMode startActionMode(ActionMode.Callback callback, int type){
    return m_vview.startActionMode(callback, type); }
@Override
public void startAnimation(Animation animation){
    m_vview.startAnimation(animation); }
@Override
public boolean startNestedScroll(int axes){
    return m_vview.startNestedScroll(axes); }
@Override
public void stopNestedScroll(){ m_vview.stopNestedScroll(); }
@Override
public String toString(){ return m_vview.toString(); }
@Override
public void unscheduleDrawable(Drawable who){
    m_vview.unscheduleDrawable(who); }
@Override
public void unscheduleDrawable(Drawable who, Runnable what){
    m_vview.unscheduleDrawable(who, what); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean willNotCacheDrawing(){ return m_vview.willNotCacheDrawing(); }
@Override
@ViewDebug.ExportedProperty(category = "drawing")
public boolean willNotDraw(){ return m_vview.willNotDraw(); }
public videoview_proxy(Context context, AttributeSet attrs, int defStyleAttr){
    super(context, attrs, defStyleAttr); }
public videoview_proxy(Context context, AttributeSet attrs, int defStyleAttr,
    int defStyleRes)
{
    super(context, attrs, defStyleAttr, defStyleRes);
}
}

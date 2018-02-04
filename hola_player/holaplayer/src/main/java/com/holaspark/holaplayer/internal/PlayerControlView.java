package com.holaspark.holaplayer.internal;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.ui.DefaultTimeBar;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.holaspark.holaplayer.BuildConfig;
import com.holaspark.holaplayer.HolaPlayer;
import com.holaspark.holaplayer.R;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerControlView extends PlaybackControlView {

private ExoPlayerController m_controller;
private HolaPlayer m_hola_player;
private Map<MenuItem, QualityItem> m_quality_map;
private Player.DefaultEventListener m_player_listener;
private View m_live_control;
private View m_position;
private View m_duration;
private DefaultTimeBar m_progress;
public PlayerControlView(Context context){
    this(context, null);
}

public PlayerControlView(Context context, AttributeSet attrs){
    this(context, attrs, 0);
}

public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr){
    this(context, attrs, defStyleAttr, attrs);
}

public PlayerControlView(Context context, AttributeSet attrs, int defStyleAttr,
    AttributeSet playbackAttrs){
    super(context, attrs, defStyleAttr, playbackAttrs);
    m_live_control = findViewById(R.id.live_control);
    m_position = findViewById(R.id.exo_position);
    m_duration = findViewById(R.id.exo_duration);
    m_progress = findViewById(R.id.exo_progress);
    m_quality_map = new HashMap<>();
    findViewById(R.id.hola_player_menu_button).setOnClickListener(new OnClickListener(){
        @Override
        public void onClick(View v){ show_settings_menu(v); }
    });
    // XXX andrey: add separate fullscreen_exit button
    findViewById(R.id.hola_fullscreen_button).setOnClickListener(new OnClickListener(){
        @Override
        public void onClick(View v){ m_hola_player.fullscreen(null); }
    });
    m_player_listener = new Player.DefaultEventListener() {
        @Override
        public void onPlayerStateChanged(boolean playing, int state){ set_auto_hide(playing); }
        @Override
        public void onTimelineChanged(Timeline timeline, Object manifest){
            boolean live = getPlayer().isCurrentWindowDynamic();
            m_live_control.setVisibility(live ? VISIBLE : GONE);
            m_position.setVisibility(live ? GONE : VISIBLE);
            m_duration.setVisibility(live ? GONE : VISIBLE);
            m_progress.setVisibility(live ? GONE : VISIBLE);
        }
    };
}

@Override
public void setPlayer(Player player) {
    Player old = getPlayer();
    super.setPlayer(player);
    if (old==player)
        return;
    if (old!=null)
        old.removeListener(m_player_listener);
    if (player!=null)
        player.addListener(m_player_listener);
}

private void show_settings_menu(View v){
    PopupMenu popup = new PopupMenu(getContext(), v);
    Menu menu = popup.getMenu();
    MenuInflater inflater = popup.getMenuInflater();
    inflater.inflate(R.menu.settings, menu);
    populate_speed_menu(menu.findItem(R.id.speed_menu_item));
    populate_quality_menu(menu.findItem(R.id.quality_menu_item));
    menu.findItem(R.id.powered_by_hola).setTitle(getResources().getString(R.string.powered_by_hola)+
        " "+BuildConfig.VERSION_NAME);
    force_popup_menu_icons(popup);
    popup.setOnDismissListener(new PopupMenu.OnDismissListener(){
        @Override
        public void onDismiss(PopupMenu menu){ set_auto_hide(true); }
    });
    popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
        @Override
        public boolean onMenuItemClick(MenuItem item){
            if (item.getItemId()==R.id.powered_by_hola)
                return false;
            if (item.getGroupId()==R.id.hola_speed_menu)
                set_speed(item);
            else if (item.getGroupId()==R.id.hola_quality_menu)
                set_quality(item);
            return true;
        }
    });
    popup.show();
    set_auto_hide(false);
}

private void set_speed(MenuItem item){
    String title = item.getTitle().toString();
    float rate = title.equals(getResources().getString(R.string.normal)) ? 1 :
        Float.parseFloat(title.replace("x", ""));
    getPlayer().setPlaybackParameters(new PlaybackParameters(rate, rate));
}

private void set_quality(MenuItem item){
    m_controller.set_quality(m_quality_map.get(item));
}

private void populate_quality_menu(MenuItem menu_item){
    Menu menu = menu_item.getSubMenu();
    m_quality_map.clear();
    menu.clear();
    List<QualityItem> items = new ArrayList<>(m_controller.get_quality_items());
    Collections.sort(items, new QualityItem.BitrateComparator());
    if (items.size()<2)
    {
        menu_item.setVisible(false);
        return;
    }
    menu_item.setVisible(true);
    int group = R.id.hola_quality_menu;
    menu.add(group, Menu.NONE, Menu.NONE, getResources().getString(R.string.auto));
    for (QualityItem item : items)
        m_quality_map.put(menu.add(group, Menu.NONE, Menu.NONE, item.toString()), item);
}

private void populate_speed_menu(MenuItem menu_item){
    Menu menu = menu_item.getSubMenu();
    menu.clear();
    String[] rates = {"0.25", "0.5", "0.75", "1", "1.25", "1.5", "2"};
    String normal = getResources().getString(R.string.normal);
    int group = R.id.hola_speed_menu;
    for (String rate : rates)
        menu.add(group, Menu.NONE, Menu.NONE, rate.equals("1") ? normal: rate+"x");
}

// XXX andrey: find a way to show icons without reflection
public static void force_popup_menu_icons(PopupMenu popupMenu){
    try {
        Field[] fields = popupMenu.getClass().getDeclaredFields();
        for (Field field : fields)
        {
            if ("mPopup".equals(field.getName()))
            {
                field.setAccessible(true);
                Object menuPopupHelper = field.get(popupMenu);
                Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon",
                    boolean.class);
                setForceIcons.invoke(menuPopupHelper, true);
                break;
            }
        }
    } catch (Throwable e){ e.printStackTrace(); }
}

public void set_auto_hide(boolean val){
    setShowTimeoutMs(val ? DEFAULT_SHOW_TIMEOUT_MS : -1);
    if (isVisible())
        show();
}

public void set_player_controller(ExoPlayerController controller){ m_controller = controller; }
public void set_hola_player(HolaPlayer player){ m_hola_player = player; }
}
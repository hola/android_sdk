package com.holaspark.holaplayer.internal;
import android.view.View;
import android.view.ViewGroup;
public class PlayerViewManager {
private ViewGroup m_parent;
private int m_parent_index;
private ViewGroup.LayoutParams m_layout_params;
public void detach(View player){
    m_parent = (ViewGroup)player.getParent();
    m_parent_index = m_parent.indexOfChild(player);
    m_layout_params = player.getLayoutParams();
    m_parent.removeViewAt(m_parent_index);
}
public void restore(View player){
    ((ViewGroup) player.getParent()).removeView(player);
    m_parent.addView(player, m_parent_index, m_layout_params);
}
}

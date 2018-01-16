package com.holaspark.holaplayer.internal;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;
public class VideoFrameLayout extends FrameLayout {
private float m_aspect = 0.0f;
public VideoFrameLayout(Context context, AttributeSet attr){
    super(context, attr); }
public void set_aspect(float aspect){ m_aspect = aspect; }
@Override
protected void onMeasure(int width, int height){
    super.onMeasure(width, height);
    width = getMeasuredWidth();
    height = getMeasuredHeight();
    if (width==0 && height==0)
        return;
    if (width==0 || height==0)
    {
        width = width>0 ? width : (int)(height*m_aspect);
        height = height>0 ? height : (int)(width/m_aspect);
        Log.d(Const.TAG, "measured_z "+width+" "+height);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        return;
    }
    float view_aspect = (float) width/height;
    if (Math.abs(m_aspect/view_aspect-1)<=0.01f)
        return;
    if (m_aspect>view_aspect)
        height = (int)(width/m_aspect);
    else
        width = (int)(height*m_aspect);
    Log.d(Const.TAG, "measured "+width+" "+height);
    super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
        MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
}
}

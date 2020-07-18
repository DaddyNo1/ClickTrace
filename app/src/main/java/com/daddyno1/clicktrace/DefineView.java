package com.daddyno1.clicktrace;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class DefineView extends View {

    public DefineView(Context context) {
        super(context);
    }

    public DefineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DefineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setColor(Color.RED);

        canvas.drawRect(0, 0,300,300, paint);
    }
}

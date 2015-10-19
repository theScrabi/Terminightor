package org.schabi.terminightor;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by the-scrabi on 16.09.15.
 *
 * Copyright (C) Christian Schabesberger 2015 <chris.schabesberger@mailbox.org>
 * ChooseDaysView.java is part of Terminightor.
 *
 * Terminightor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Terminightor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Terminightor.  If not, see <http://www.gnu.org/licenses/>.
 */

public class ChooseDaysView extends View {

    private static final String TAG = ChooseDaysView.class.toString();

    private float width;
    private float height;
    private float buttonWidth;
    private float buttonRadius;
    private static final int STATIC_HIGHT = 50;
    private final float scale = getResources().getDisplayMetrics().density;

    private final int WHITE_COLOR = getResources().getColor(android.R.color.white);
    private final int DARK_COLOR = getResources().getColor(R.color.generalColor);
    private Paint buttonPaint;
    private Paint textPaint;
    private Rect textBounds = new Rect();
    private String daysOfWeek = "";

    // 7: repeat, 6: saturday, 0: monday
    private int enabledDays = 0b10011111;

    public OnDaysChangedListener listener = null;
    public interface OnDaysChangedListener {
        void daysChanged(int enabledDays);
    }

    public ChooseDaysView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ChooseDaysView, 0, 0);
        try {
            daysOfWeek = a.getString(R.styleable.ChooseDaysView_days_of_week);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        width = w;
        height = h;
        buttonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        buttonPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(20 * scale);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.getTextBounds(daysOfWeek, 0, daysOfWeek.length(), textBounds);
        buttonWidth = width/7;
        buttonRadius = (buttonWidth > height ? height/2 : buttonWidth/2) - (8 * scale);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = 0;
        switch(MeasureSpec.getMode(heightMeasureSpec)) {
            case MeasureSpec.AT_MOST:
                h = MeasureSpec.getSize(heightMeasureSpec);
                if(h > STATIC_HIGHT * scale) {
                    h = (int)(STATIC_HIGHT * scale);
                }
                break;
            case MeasureSpec.EXACTLY:
                h = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.UNSPECIFIED:
                h = (int)(STATIC_HIGHT * scale);
                break;
        }
        setMeasuredDimension(w, h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            int pressedButton = (int)(event.getX() / buttonWidth);
            if((enabledDays & (1 << pressedButton)) >= 1) {
                enabledDays &= ~(1 << pressedButton);
            } else {
                enabledDays |= (1 << pressedButton);
            }
        }
        if(listener != null) {
            listener.daysChanged(enabledDays);
        }
        invalidate();
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for(int i = 0 ; i < 7; i++) {
            if((enabledDays & (1 << i)) != 0) {
                buttonPaint.setColor(WHITE_COLOR);
                textPaint.setColor(DARK_COLOR);
            } else {
                buttonPaint.setColor(DARK_COLOR);
                textPaint.setColor(WHITE_COLOR);
            }
            canvas.drawCircle(i*buttonWidth + buttonWidth/2, height/2, buttonRadius, buttonPaint);
            canvas.drawText(daysOfWeek.substring(i, i+1),
                    (i*buttonWidth + buttonWidth/2),
                    (height/2) - textBounds.exactCenterY(), textPaint);
        }
    }

    public void setEnabledDays(int enabledDays) {
        this.enabledDays = enabledDays;
    }

    public int getEnabledDays() {
        return this.enabledDays;
    }

    public void setRepeatEnabled(boolean repeat) {
        if(repeat) {
            this.enabledDays |= (1<<7);
        } else {
            this.enabledDays &= ~(1<<7);
        }
    }

    public boolean isRepeatEnabled() {
        return (this.enabledDays & (1<<7)) >= 1;
    }

    public void setOnDaysChangedListener(OnDaysChangedListener onDaysChangedListener) {
        listener = onDaysChangedListener;
    }
}

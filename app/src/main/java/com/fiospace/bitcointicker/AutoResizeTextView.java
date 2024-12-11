package com.fiospace.bitcointicker;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;

public class AutoResizeTextView extends androidx.appcompat.widget.AppCompatTextView {
    private Rect boundsRect = new Rect();
    private Rect textRect = new Rect();

    public AutoResizeTextView(Context context) {
        super(context);
        init();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoResizeTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setPadding(0, 0, 0, 0);
        setIncludeFontPadding(false);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int before, int after) {
        super.onTextChanged(text, start, before, after);
        adjustTextSize();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        adjustTextSize();
    }

    private void adjustTextSize() {
        System.out.println("adjusting text size");

        if (getWidth() <= 0 || getHeight() <= 0) return;

        boundsRect.set(0, 0, getWidth(), getHeight());
        float textSize = getTextSize();

        getPaint().getTextBounds(getText().toString(), 0, getText().length(), textRect);

        while ((textRect.width() > boundsRect.width() || textRect.height() > boundsRect.height()) && textSize > 0) {
            textSize--;
            setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            getPaint().getTextBounds(getText().toString(), 0, getText().length(), textRect);
            System.out.println("adjusting text size " + textSize);

        }
    }
}

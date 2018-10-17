
package com.bozapro.circularsliderrange;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class CircularSliderRange extends View {

    /**
     * Listener interface used to detect when slider moves around.
     */
    public interface OnSliderRangeMovedListener {

        /**
         * This method is invoked when start thumb is moved, providing position of the start slider thumb.
         *
         * @param pos Value between 0 and 1 representing the current angle.<br>
         *            {@code pos = (Angle - StartingAngle) / (2 * Pi)}
         */
        void onStartSliderMoved(double pos);

        /**
         * This method is invoked when end thumb is moved, providing position of the end slider thumb.
         *
         * @param pos Value between 0 and 1 representing the current angle.<br>
         *            {@code pos = (Angle - StartingAngle) / (2 * Pi)}
         */
        void onEndSliderMoved(double pos);

        /**
         * This method is invoked when start slider is pressed/released.
         *
         * @param event Event represent state of the slider, it can be in two states: Pressed or Released.
         */
        void onStartSliderEvent(ThumbEvent event);

        /**
         * This method is invoked when end slider is pressed/released.
         *
         * @param event Event represent state of the slider, it can be in two states: Pressed or Released.
         */
        void onEndSliderEvent(ThumbEvent event);
    }

    private int mThumbStartX;
    private int mThumbStartY;

    private int mThumbEndX;
    private int mThumbEndY;

    private int mCircleCenterX;
    private int mCircleCenterY;
    private int mCircleRadius;

    private Drawable mStartThumbImage;
    private Drawable mEndThumbImage;
    private int mPadding;
    private int mStartThumbSize;
    private int mEndThumbSize;
    private int mStartThumbColor;
    private int mEndThumbColor;
    private int mBorderColor;
    private int mBorderThickness;
    private int mArcDashSize;
    private int mArcColor;
    private LineCap mLineCap;
    private double mAngle;
    private double mAngleEnd;
    private boolean mIsThumbSelected = false;
    private boolean mIsThumbEndSelected = false;

    private Paint mPaint = new Paint();
    private Paint mLinePaint = new Paint();
    private RectF arcRectF = new RectF();
    private Rect arcRect = new Rect();
    private OnSliderRangeMovedListener mListener;
    private static final int THUMB_SIZE_NOT_DEFINED = -1;

    private enum Thumb {
        START, END
    }

    public enum LineCap {
        BUTT(0),
        ROUND(1),
        SQUARE(2);

        int id;

        LineCap(int id) {
            this.id = id;
        }

        static LineCap fromId(int id) {
            for (LineCap lc : values()) {
                if (lc.id == id) return lc;
            }
            throw new IllegalArgumentException();
        }

        public Paint.Cap getPaintCap() {
            switch (this) {
                case BUTT:
                default:
                    return Paint.Cap.BUTT;
                case ROUND:
                    return Paint.Cap.ROUND;
                case SQUARE:
                    return Paint.Cap.SQUARE;
            }
        }
    }

    public CircularSliderRange(Context context) {
        this(context, null);
    }

    public CircularSliderRange(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CircularSliderRange(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CircularSliderRange(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr);
    }

    // common initializer method
    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CircularSlider, defStyleAttr, 0);

        // read all available attributes
        float startAngle = a.getFloat(R.styleable.CircularSlider_start_angle, 90);
        float endAngle = a.getFloat(R.styleable.CircularSlider_end_angle, 60);
        int thumbSize = a.getDimensionPixelSize(R.styleable.CircularSlider_thumb_size, 50);
        int startThumbSize = a.getDimensionPixelSize(R.styleable.CircularSlider_start_thumb_size, THUMB_SIZE_NOT_DEFINED);
        int endThumbSize = a.getDimensionPixelSize(R.styleable.CircularSlider_end_thumb_size, THUMB_SIZE_NOT_DEFINED);
        int thumbColor = a.getColor(R.styleable.CircularSlider_start_thumb_color, Color.GRAY);
        int thumbEndColor = a.getColor(R.styleable.CircularSlider_end_thumb_color, Color.GRAY);
        int borderThickness = a.getDimensionPixelSize(R.styleable.CircularSlider_border_thickness, 20);
        int arcDashSize = a.getDimensionPixelSize(R.styleable.CircularSlider_arc_dash_size, 60);
        int arcColor = a.getColor(R.styleable.CircularSlider_arc_color, 0);
        int borderColor = a.getColor(R.styleable.CircularSlider_border_color, Color.RED);
        Drawable thumbImage = a.getDrawable(R.styleable.CircularSlider_start_thumb_image);
        Drawable thumbEndImage = a.getDrawable(R.styleable.CircularSlider_end_thumb_image);
        LineCap lineCap = LineCap.fromId(a.getInt(R.styleable.CircularSlider_line_cap, 0));

        // save those to fields (really, do we need setters here..?)
        setStartAngle(startAngle);
        setEndAngle(endAngle);
        setBorderThickness(borderThickness);
        setBorderColor(borderColor);
        setThumbSize(thumbSize);
        setStartThumbSize(startThumbSize);
        setEndThumbSize(endThumbSize);
        setStartThumbImage(thumbImage);
        setEndThumbImage(thumbEndImage);
        setStartThumbColor(thumbColor);
        setEndThumbColor(thumbEndColor);
        setArcColor(arcColor);
        setArcDashSize(arcDashSize);
        setLineCap(lineCap);

        // assign padding - check for version because of RTL layout compatibility
        int padding;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            int all = getPaddingLeft() + getPaddingRight() + getPaddingBottom() + getPaddingTop() + getPaddingEnd() + getPaddingStart();
            padding = all / 6;
        } else {
            padding = (getPaddingLeft() + getPaddingRight() + getPaddingBottom() + getPaddingTop()) / 4;
        }
        setPadding(padding);
        a.recycle();

        if (isInEditMode())
            return;
    }

    /* ***** Setters ***** */

    /**
     * Set start angle in degrees.
     * An angle of 0 degrees correspond to the geometric angle of 0 degrees (3 o'clock on a watch.)
     *
     * @param startAngle value in degrees.
     */
    public void setStartAngle(double startAngle) {
        mAngle = fromDrawingAngle(startAngle);
    }

    /**
     * Set end angle in degrees.
     * An angle of 0 degrees correspond to the geometric angle of 0 degrees (3 o'clock on a watch.)
     *
     * @param angle value in degrees.
     */
    public void setEndAngle(double angle) {
        mAngleEnd = fromDrawingAngle(angle);
    }

    public void setThumbSize(int thumbSize) {
        setStartThumbSize(thumbSize);
        setEndThumbSize(thumbSize);
    }

    public void setStartThumbSize(int thumbSize) {
        if (thumbSize == THUMB_SIZE_NOT_DEFINED)
            return;
        mStartThumbSize = thumbSize;
    }

    public void setEndThumbSize(int thumbSize) {
        if (thumbSize == THUMB_SIZE_NOT_DEFINED)
            return;
        mEndThumbSize = thumbSize;
    }

    public int getStartThumbSize() {
        return mStartThumbSize;
    }

    public int getEndThumbSize() {
        return mEndThumbSize;
    }

    public void setBorderThickness(int circleBorderThickness) {
        mBorderThickness = circleBorderThickness;
    }

    public void setBorderColor(int color) {
        mBorderColor = color;
    }

    public void setStartThumbImage(Drawable drawable) {
        mStartThumbImage = drawable;
    }

    public void setEndThumbImage(Drawable drawable) {
        mEndThumbImage = drawable;
    }

    public void setStartThumbColor(int color) {
        mStartThumbColor = color;
    }

    public void setEndThumbColor(int color) {
        mEndThumbColor = color;
    }

    public void setPadding(int padding) {
        mPadding = padding;
    }

    public void setArcColor(int color) {
        mArcColor = color;
    }

    public void setArcDashSize(int value) {
        mArcDashSize = value;
    }

    public void setLineCap(LineCap value) { mLineCap = value; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // use smaller dimension for calculations (depends on parent size)
        int smallerDim = w > h ? h : w;

        // find circle's rectangle points
        int largestCenteredSquareLeft = (w - smallerDim) / 2;
        int largestCenteredSquareTop = (h - smallerDim) / 2;
        int largestCenteredSquareRight = largestCenteredSquareLeft + smallerDim;
        int largestCenteredSquareBottom = largestCenteredSquareTop + smallerDim;

        // save circle coordinates and radius in fields
        mCircleCenterX = largestCenteredSquareRight / 2 + (w - largestCenteredSquareRight) / 2;
        mCircleCenterY = largestCenteredSquareBottom / 2 + (h - largestCenteredSquareBottom) / 2;
        mCircleRadius = smallerDim / 2 - mBorderThickness / 2 - mPadding;

        // works well for now, should we call something else here?
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // outer circle (ring)
        mPaint.setColor(mBorderColor);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(mBorderThickness);
        mPaint.setAntiAlias(true);
        canvas.drawCircle(mCircleCenterX, mCircleCenterY, mCircleRadius, mPaint);

        // find thumb start position
        mThumbStartX = (int) (mCircleCenterX + mCircleRadius * Math.cos(mAngle));
        mThumbStartY = (int) (mCircleCenterY - mCircleRadius * Math.sin(mAngle));

        //find thumb end position
        mThumbEndX = (int) (mCircleCenterX + mCircleRadius * Math.cos(mAngleEnd));
        mThumbEndY = (int) (mCircleCenterY - mCircleRadius * Math.sin(mAngleEnd));

        mLinePaint.setColor(mArcColor == 0 ? Color.RED : mArcColor);
        mLinePaint.setStyle(Paint.Style.STROKE);
        mLinePaint.setStrokeWidth(mArcDashSize);
        mLinePaint.setAntiAlias(true);
        mLinePaint.setTextSize(50);
        mLinePaint.setStrokeCap(mLineCap.getPaintCap());

        arcRect.set(mCircleCenterX - mCircleRadius, mCircleCenterY + mCircleRadius, mCircleCenterX + mCircleRadius, mCircleCenterY - mCircleRadius);
        arcRectF.set(arcRect);
        arcRectF.sort();

        final float drawStart = toDrawingAngle(mAngle);
        final float drawEnd = toDrawingAngle(mAngleEnd);

        canvas.drawArc(arcRectF, drawStart, (360 + drawEnd - drawStart) % 360, false, mLinePaint);
        int mThumbSize = getStartThumbSize();
        if (mStartThumbImage != null) {
            // draw png
            mStartThumbImage.setBounds(mThumbStartX - mThumbSize / 2, mThumbStartY - mThumbSize / 2, mThumbStartX + mThumbSize / 2, mThumbStartY + mThumbSize / 2);
            mStartThumbImage.draw(canvas);
        } else {
            // draw colored circle
            mPaint.setColor(mStartThumbColor);
            mPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(mThumbStartX, mThumbStartY, mThumbSize / 2, mPaint);

            //helper text, used for debugging
            //mLinePaint.setStrokeWidth(5);
            //canvas.drawText(String.format(Locale.US, "%.1f", drawStart), mThumbStartX - 20, mThumbStartY, mLinePaint);
            //canvas.drawText(String.format(Locale.US, "%.1f", drawEnd), mThumbEndX - 20, mThumbEndY, mLinePaint);
        }

        mThumbSize = getEndThumbSize();
        if (mEndThumbImage != null) {
            // draw png
            mEndThumbImage.setBounds(mThumbEndX - mThumbSize / 2, mThumbEndY - mThumbSize / 2, mThumbEndX + mThumbSize / 2, mThumbEndY + mThumbSize / 2);
            mEndThumbImage.draw(canvas);
        } else {
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(mEndThumbColor);
            canvas.drawCircle(mThumbEndX, mThumbEndY, mThumbSize / 2, mPaint);
        }
    }

    /**
     * Invoked when slider starts moving or is currently moving. This method calculates and sets position and angle of the thumb.
     *
     * @param touchX Where is the touch identifier now on X axis
     * @param touchY Where is the touch identifier now on Y axis
     */
    private void updateSliderState(int touchX, int touchY, Thumb thumb) {
        int distanceX = touchX - mCircleCenterX;
        int distanceY = mCircleCenterY - touchY;
        //noinspection SuspiciousNameCombination
        double c = Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
        double angle = Math.acos(distanceX / c);
        if (distanceY < 0)
            angle = -angle;

        if (thumb == Thumb.START) {
            mAngle = angle;
        } else {
            mAngleEnd = angle;
        }

        if (mListener != null) {

            if (thumb == Thumb.START) {
                mListener.onStartSliderMoved(toDrawingAngle(angle));
            } else {
                mListener.onEndSliderMoved(toDrawingAngle(angle));
            }
        }
    }

    private float toDrawingAngle(double angleInRadians) {
        double fixedAngle = Math.toDegrees(angleInRadians);
        if (angleInRadians > 0)
            fixedAngle = 360 - fixedAngle;
        else
            fixedAngle = -fixedAngle;
        return (float) fixedAngle;
    }

    private double fromDrawingAngle(double angleInDegrees) {
        double radians = Math.toRadians(angleInDegrees);
        return -radians;
    }

    /**
     * Set slider range moved listener. Set {@link OnSliderRangeMovedListener} to {@code null} to remove it.
     *
     * @param listener Instance of the slider range moved listener, or null when removing it
     */
    public void setOnSliderRangeMovedListener(OnSliderRangeMovedListener listener) {
        mListener = listener;
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                // start moving the thumb (this is the first touch)
                int x = (int) ev.getX();
                int y = (int) ev.getY();

                int mThumbSize = getStartThumbSize();
                boolean isThumbStartPressed = x < mThumbStartX + mThumbSize
                        && x > mThumbStartX - mThumbSize
                        && y < mThumbStartY + mThumbSize
                        && y > mThumbStartY - mThumbSize;

                mThumbSize = getEndThumbSize();
                boolean isThumbEndPressed = x < mThumbEndX + mThumbSize
                        && x > mThumbEndX - mThumbSize
                        && y < mThumbEndY + mThumbSize
                        && y > mThumbEndY - mThumbSize;

                if (isThumbStartPressed) {
                    mIsThumbSelected = true;
                    updateSliderState(x, y, Thumb.START);
                } else if (isThumbEndPressed) {
                    mIsThumbEndSelected = true;
                    updateSliderState(x, y, Thumb.END);
                }

                if (mListener != null) {
                    if (mIsThumbSelected)
                        mListener.onStartSliderEvent(ThumbEvent.THUMB_PRESSED);
                    if (mIsThumbEndSelected)
                        mListener.onEndSliderEvent(ThumbEvent.THUMB_PRESSED);
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                // still moving the thumb (this is not the first touch)
                if (mIsThumbSelected) {
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    updateSliderState(x, y, Thumb.START);
                } else if (mIsThumbEndSelected) {
                    int x = (int) ev.getX();
                    int y = (int) ev.getY();
                    updateSliderState(x, y, Thumb.END);
                }
                break;
            }

            case MotionEvent.ACTION_UP: {
                if (mListener != null) {
                    if (mIsThumbSelected)
                        mListener.onStartSliderEvent(ThumbEvent.THUMB_RELEASED);
                    if (mIsThumbEndSelected)
                        mListener.onEndSliderEvent(ThumbEvent.THUMB_RELEASED);
                }

                // finished moving (this is the last touch)
                mIsThumbSelected = false;
                mIsThumbEndSelected = false;
                break;
            }
        }

        invalidate();
        return true;
    }

}

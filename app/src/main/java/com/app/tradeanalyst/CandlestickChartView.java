package com.tradeanalyst.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class CandlestickChartView extends View {

    public static class CustomIndicatorLine {
        public String label;
        public double level;
        public int color;

        public CustomIndicatorLine(String label, double level, int color) {
            this.label = label;
            this.level = level;
            this.color = color;
        }
    }

    private List<Candlestick> mCandles = new ArrayList<>();
    private List<CustomIndicatorLine> mCustomLines = new ArrayList<>();
    private List<PatternDrawingModel> mPatterns = new ArrayList<>();
    
    // Toggleable built-in indicators
    private boolean mShowEMA20 = true;
    private boolean mShowSMA20 = true;
    private boolean mShowBollingerBands = true;
    private boolean mShowSupportResistance = true;
    private ChartPatternResponse mActivePatternResponse = null;
    private boolean mHideAllIndicators = false;

    // Scroll Listener for Dynamic Historic Loading
    public interface OnScrollToLeftListener {
        void onScrollToLeft();
    }
    private OnScrollToLeftListener mOnScrollToLeftListener;

    public void setOnScrollToLeftListener(OnScrollToLeftListener listener) {
        mOnScrollToLeftListener = listener;
    }

    public void notifyHistoryPrepended(int count) {
        mOffsetX += count * mCandleWidth;
        invalidate();
    }

    // Paints
    private Paint mCandleUpPaint;
    private Paint mCandleDownPaint;
    private Paint mLinePaint;
    private Paint mTextPaint;
    private Paint mEmaPaint;
    private Paint mSmaPaint;
    private Paint mBbPaint;
    private Paint mSrPaint;
    
    // Colors
    private int mBgColor = Color.parseColor("#060B08"); // Velvet Dark Emerald Default
    private int mGridColor = Color.parseColor("#1B2F25");

    // Gesture & Viewport View States
    private float mOffsetX = 0f; // Scroll/Pan offset from left side in pixels
    private float mLastTouchX = 0f;
    private boolean mIsDragging = false;
    
    private float mCandleWidth = 25f; // Dynamic candlestick width (pixels per candle)
    private static final float MIN_CANDLE_WIDTH = 1.5f;
    private static final float MAX_CANDLE_WIDTH = 180f;
    private ScaleGestureDetector mScaleDetector;

    public CandlestickChartView(Context context) {
        super(context);
        init(context);
    }

    public CandlestickChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mCandleUpPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCandleUpPaint.setColor(Color.parseColor("#10B981")); // Emerald Green
        mCandleUpPaint.setStyle(Paint.Style.FILL);

        mCandleDownPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCandleDownPaint.setColor(Color.parseColor("#EF4444")); // Red Rose
        mCandleDownPaint.setStyle(Paint.Style.FILL);

        mLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinePaint.setStrokeWidth(2f);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setTextSize(24f);

        mEmaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mEmaPaint.setColor(Color.parseColor("#3B82F6")); // Blue EMA
        mEmaPaint.setStrokeWidth(3f);
        mEmaPaint.setStyle(Paint.Style.STROKE);

        mSmaPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSmaPaint.setColor(Color.parseColor("#F59E0B")); // Orange SMA
        mSmaPaint.setStrokeWidth(3f);
        mSmaPaint.setStyle(Paint.Style.STROKE);

        mBbPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBbPaint.setColor(Color.parseColor("#8B5CF6")); // Purple BB
        mBbPaint.setStrokeWidth(2f);
        mBbPaint.setStyle(Paint.Style.STROKE);

        mSrPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSrPaint.setColor(Color.parseColor("#14B8A6")); // Teal support/resistance
        mSrPaint.setStrokeWidth(2f);
        mSrPaint.setStyle(Paint.Style.STROKE);
        mSrPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));

        // Initialize Scale gesture listener to support clean pinch-to-zoom actions
        mScaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float prevCandleWidth = mCandleWidth;
                mCandleWidth *= detector.getScaleFactor();
                mCandleWidth = Math.max(MIN_CANDLE_WIDTH, Math.min(MAX_CANDLE_WIDTH, mCandleWidth));

                float focusX = detector.getFocusX();
                float totalWidthNew = mCandles.size() * mCandleWidth;

                // Adjust horizontal offset to scale coordinates directly relative to touch focus centerpoint
                mOffsetX = ((mOffsetX + focusX) * (mCandleWidth / prevCandleWidth)) - focusX;

                // Restrict zoom limits to prevent canvas overflows
                float maxOffset = Math.max(0f, totalWidthNew - getWidth());
                if (mOffsetX < 0f) mOffsetX = 0f;
                if (mOffsetX > maxOffset) mOffsetX = maxOffset;

                invalidate();
                return true;
            }
        });
    }

    public void setTheme(boolean isDark) {
        if (isDark) {
            mBgColor = Color.parseColor("#060B08"); // Velvet Dark Emerald
            mGridColor = Color.parseColor("#1B2F25");
            mTextPaint.setColor(Color.parseColor("#A3B899"));
        } else {
            mBgColor = Color.parseColor("#F2F6F3"); // Ivory Sage Light
            mGridColor = Color.parseColor("#D4DDD6");
            mTextPaint.setColor(Color.parseColor("#1E382A"));
        }
        invalidate();
    }

    public void setCandles(List<Candlestick> candles) {
        mCandles = candles;
        
        // Snap-scroll view completely to the right edge to show the latest live market values
        post(() -> {
            float totalWidth = mCandles.size() * mCandleWidth;
            mOffsetX = Math.max(0f, totalWidth - getWidth());
            invalidate();
        });
    }

    public List<Candlestick> getCandles() {
        return mCandles;
    }

    public void setPatterns(List<PatternDrawingModel> patterns) {
        if (patterns != null) {
            mPatterns = patterns;
        } else {
            mPatterns.clear();
        }
        invalidate();
    }

    public List<PatternDrawingModel> getPatterns() {
        return mPatterns;
    }

    public float getOffsetX() {
        return mOffsetX;
    }

    public float getCandleWidth() {
        return mCandleWidth;
    }

    public double getViewportMinPrice() {
        double minPrice = Double.MAX_VALUE;
        int numCandles = mCandles.size();
        if (numCandles == 0) return 0.0;

        int firstIndex = 0;
        int lastIndex = numCandles - 1;
        float width = getWidth();
        if (width > 0 && mCandleWidth > 0) {
            firstIndex = Math.max(0, (int) (mOffsetX / mCandleWidth) - 1);
            lastIndex = Math.min(numCandles - 1, (int) ((mOffsetX + width) / mCandleWidth) + 1);
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            Candlestick candle = mCandles.get(i);
            if (candle.low < minPrice) minPrice = candle.low;
        }
        if (!mHideAllIndicators && mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (bb.lowerBand[i] < minPrice && bb.lowerBand[i] > 0) minPrice = bb.lowerBand[i];
            }
        }
        if (!mHideAllIndicators) {
            for (CustomIndicatorLine line : mCustomLines) {
                if (line.level < minPrice) minPrice = line.level;
            }
        }
        double maxPrice = getViewportMaxPriceRaw();
        double priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1.0;
        return minPrice - priceRange * 0.05;
    }

    public double getViewportMaxPrice() {
        double maxPrice = Double.MIN_VALUE;
        int numCandles = mCandles.size();
        if (numCandles == 0) return 1.0;

        int firstIndex = 0;
        int lastIndex = numCandles - 1;
        float width = getWidth();
        if (width > 0 && mCandleWidth > 0) {
            firstIndex = Math.max(0, (int) (mOffsetX / mCandleWidth) - 1);
            lastIndex = Math.min(numCandles - 1, (int) ((mOffsetX + width) / mCandleWidth) + 1);
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            Candlestick candle = mCandles.get(i);
            if (candle.high > maxPrice) maxPrice = candle.high;
        }
        if (!mHideAllIndicators && mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (bb.upperBand[i] > maxPrice) maxPrice = bb.upperBand[i];
            }
        }
        if (!mHideAllIndicators) {
            for (CustomIndicatorLine line : mCustomLines) {
                if (line.level > maxPrice) maxPrice = line.level;
            }
        }
        double minPrice = getViewportMinPriceRaw();
        double priceRange = maxPrice - minPrice;
        if (priceRange == 0) priceRange = 1.0;
        return maxPrice + priceRange * 0.05;
    }

    private double getViewportMaxPriceRaw() {
        double maxPrice = Double.MIN_VALUE;
        int numCandles = mCandles.size();
        if (numCandles == 0) return 1.0;

        int firstIndex = 0;
        int lastIndex = numCandles - 1;
        float width = getWidth();
        if (width > 0 && mCandleWidth > 0) {
            firstIndex = Math.max(0, (int) (mOffsetX / mCandleWidth) - 1);
            lastIndex = Math.min(numCandles - 1, (int) ((mOffsetX + width) / mCandleWidth) + 1);
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            Candlestick candle = mCandles.get(i);
            if (candle.high > maxPrice) maxPrice = candle.high;
        }
        if (!mHideAllIndicators && mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (bb.upperBand[i] > maxPrice) maxPrice = bb.upperBand[i];
            }
        }
        if (!mHideAllIndicators) {
            for (CustomIndicatorLine line : mCustomLines) {
                if (line.level > maxPrice) maxPrice = line.level;
            }
        }
        return maxPrice;
    }

    private double getViewportMinPriceRaw() {
        double minPrice = Double.MAX_VALUE;
        int numCandles = mCandles.size();
        if (numCandles == 0) return 0.0;

        int firstIndex = 0;
        int lastIndex = numCandles - 1;
        float width = getWidth();
        if (width > 0 && mCandleWidth > 0) {
            firstIndex = Math.max(0, (int) (mOffsetX / mCandleWidth) - 1);
            lastIndex = Math.min(numCandles - 1, (int) ((mOffsetX + width) / mCandleWidth) + 1);
        }

        for (int i = firstIndex; i <= lastIndex; i++) {
            Candlestick candle = mCandles.get(i);
            if (candle.low < minPrice) minPrice = candle.low;
        }
        if (!mHideAllIndicators && mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = firstIndex; i <= lastIndex; i++) {
                if (bb.lowerBand[i] < minPrice && bb.lowerBand[i] > 0) minPrice = bb.lowerBand[i];
            }
        }
        if (!mHideAllIndicators) {
            for (CustomIndicatorLine line : mCustomLines) {
                if (line.level < minPrice) minPrice = line.level;
            }
        }
        return minPrice;
    }

    public void addCustomLine(String label, double level, int color) {
        mCustomLines.add(new CustomIndicatorLine(label, level, color));
        invalidate();
    }

    public void clearCustomLines() {
        mCustomLines.clear();
        invalidate();
    }

    public void setEnabledIndicators(boolean ema, boolean sma, boolean bb, boolean sr) {
        mShowEMA20 = ema;
        mShowSMA20 = sma;
        mShowBollingerBands = bb;
        mShowSupportResistance = sr;
        invalidate();
    }

    public void setActivePatternResponse(ChartPatternResponse response) {
        mActivePatternResponse = response;
        invalidate();
    }

    public ChartPatternResponse getActivePatternResponse() {
        return mActivePatternResponse;
    }

    public void setHideAllIndicators(boolean hideAll) {
        mHideAllIndicators = hideAll;
        invalidate();
    }

    public boolean isHideAllIndicators() {
        return mHideAllIndicators;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mScaleDetector.onTouchEvent(event);
        if (mScaleDetector.isInProgress()) {
            mIsDragging = false; // Disable standard drag panning during zoom gestures
            return true;
        }

        float x = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = x;
                mIsDragging = true;
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (mIsDragging) {
                    float deltaX = x - mLastTouchX;
                    mOffsetX -= deltaX; // Shift offset opposite to finger drag direction
                    mLastTouchX = x;

                    float totalWidth = mCandles.size() * mCandleWidth;
                    float maxOffset = Math.max(0f, totalWidth - getWidth());
                    
                    if (mOffsetX < 0f) mOffsetX = 0f;
                    if (mOffsetX > maxOffset) mOffsetX = maxOffset;

                    if (mOffsetX < getWidth() * 0.8f) {
                        if (mOnScrollToLeftListener != null) {
                            mOnScrollToLeftListener.onScrollToLeft();
                        }
                    }

                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsDragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(mBgColor);

        int width = getWidth();
        int height = getHeight();

        if (mCandles == null || mCandles.isEmpty()) {
            mTextPaint.setTextSize(36f);
            canvas.drawText("No Candlestick Data Loaded", width / 2f - 200, height / 2f, mTextPaint);
            return;
        }

        mTextPaint.setTextSize(24f);

        // Find min and max values to fit chart on screen
        double maxPrice = getViewportMaxPrice();
        double minPrice = getViewportMinPrice();
        double priceRange = maxPrice - minPrice;

        int numCandles = mCandles.size();
        float candleWidth = mCandleWidth;

        // Draw horizontal grid lines and prices
        int gridLinesCount = 5;
        for (int i = 0; i <= gridLinesCount; i++) {
            float y = (float) height * i / (float) gridLinesCount;
            // Map y coordinate back to price value
            double priceVal = maxPrice - (y / (float) height) * priceRange;
            
            // Grid line paint
            mLinePaint.setColor(mGridColor);
            mLinePaint.setStrokeWidth(1f);
            canvas.drawLine(0, y, width, y, mLinePaint);

            // Draw label
            canvas.drawText(String.format("$%.2f", priceVal), 15, y - 5, mTextPaint);
        }

        // Render Bollinger Bands (Lower layer)
        if (!mHideAllIndicators && mShowBollingerBands && numCandles >= 20) {
            IndicatorsEngine.BollingerBandsResult bb = IndicatorsEngine.calculateBollingerBands(mCandles);
            for (int i = 1; i < numCandles; i++) {
                float startX = (i - 1) * candleWidth + candleWidth / 2f - mOffsetX;
                float endX = i * candleWidth + candleWidth / 2f - mOffsetX;

                // Skip rendering elements that are completely off screen for optimal canvas rendering
                if (endX < 0 || startX > width) {
                    continue;
                }

                float startUpperY = getPriceY(bb.upperBand[i - 1], minPrice, maxPrice, height);
                float endUpperY = getPriceY(bb.upperBand[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startUpperY, endX, endUpperY, mBbPaint);

                float startLowerY = getPriceY(bb.lowerBand[i - 1], minPrice, maxPrice, height);
                float endLowerY = getPriceY(bb.lowerBand[i], minPrice, maxPrice, height);
                canvas.drawLine(startX, startLowerY, endX, endLowerY, mBbPaint);
            }
        }

        // Render EMA 20 and SMA 20
        if (!mHideAllIndicators) {
            double[] ema20 = IndicatorsEngine.calculateEMA(mCandles, 20);
            double[] sma20 = IndicatorsEngine.calculateSMA(mCandles, 20);

            for (int i = 1; i < numCandles; i++) {
                float startX = (i - 1) * candleWidth + candleWidth / 2f - mOffsetX;
                float endX = i * candleWidth + candleWidth / 2f - mOffsetX;

                if (endX < 0 || startX > width) {
                    continue;
                }

                if (mShowEMA20) {
                    float startEMA = getPriceY(ema20[i - 1], minPrice, maxPrice, height);
                    float endEMA = getPriceY(ema20[i], minPrice, maxPrice, height);
                    canvas.drawLine(startX, startEMA, endX, endEMA, mEmaPaint);
                }

                if (mShowSMA20) {
                    float startSMA = getPriceY(sma20[i - 1], minPrice, maxPrice, height);
                    float endSMA = getPriceY(sma20[i], minPrice, maxPrice, height);
                    canvas.drawLine(startX, startSMA, endX, endSMA, mSmaPaint);
                }
            }
        }

        // Render Support & Resistance lines
        if (!mHideAllIndicators && mShowSupportResistance) {
            List<Double> srLevels = IndicatorsEngine.findSupportResistance(mCandles);
            for (double level : srLevels) {
                float y = getPriceY(level, minPrice, maxPrice, height);
                canvas.drawLine(0, y, width, y, mSrPaint);
                canvas.drawText(String.format("S/R: $%.1f", level), width - 180, y - 5, mTextPaint);
            }
        }

        // Draw Candlesticks (Wick and Body)
        for (int i = 0; i < numCandles; i++) {
            Candlestick candle = mCandles.get(i);
            float startX = i * candleWidth - mOffsetX;
            float endX = (i + 1) * candleWidth - mOffsetX;
            float centerX = startX + candleWidth / 2f;

            if (endX < 0 || startX > width) {
                continue;
            }

            float highY = getPriceY(candle.high, minPrice, maxPrice, height);
            float lowY = getPriceY(candle.low, minPrice, maxPrice, height);
            float openY = getPriceY(candle.open, minPrice, maxPrice, height);
            float closeY = getPriceY(candle.close, minPrice, maxPrice, height);

            // Draw Wick
            mLinePaint.setColor(mTextPaint.getColor());
            mLinePaint.setStrokeWidth(Math.max(0.5f, candleWidth * 0.1f));
            canvas.drawLine(centerX, highY, centerX, lowY, mLinePaint);

            // Draw Body
            float top = Math.min(openY, closeY);
            float bottom = Math.max(openY, closeY);
            float bodyWidthFactor = 0.8f; // fill 18% space on left/right
            float bodyLeft = centerX - (candleWidth * bodyWidthFactor / 2f);
            float bodyRight = centerX + (candleWidth * bodyWidthFactor / 2f);

            // Safeguard: Ensure the body has at least a 1-pixel width, and centering is accurate
            if (bodyRight - bodyLeft < 1.0f) {
                bodyLeft = centerX - 0.5f;
                bodyRight = centerX + 0.5f;
            }

            // Prevent hairline bodies from vanishing
            if (Math.abs(top - bottom) < 2) {
                bottom = top + 2;
            }

            if (candle.close >= candle.open) {
                canvas.drawRect(bodyLeft, top, bodyRight, bottom, mCandleUpPaint);
            } else {
                canvas.drawRect(bodyLeft, top, bodyRight, bottom, mCandleDownPaint);
            }
        }

        // Draw custom (AI-driven) indicator lines
        if (!mHideAllIndicators) {
            for (CustomIndicatorLine line : mCustomLines) {
                float y = getPriceY(line.level, minPrice, maxPrice, height);
                mLinePaint.setColor(line.color);
                mLinePaint.setStrokeWidth(3f);
                canvas.drawLine(0, y, width, y, mLinePaint);

                Paint textP = new Paint(Paint.ANTI_ALIAS_FLAG);
                textP.setColor(line.color);
                textP.setTextSize(24f);
                canvas.drawText(line.label + String.format(" ($%.1f)", line.level), 15, y - 8, textP);
            }
        }

        // RENDER ACTIVE AI CHART PATTERNS (Geometric Overlays calculated 100% dynamically to prevent zooming shift)
        if (!mHideAllIndicators && mActivePatternResponse != null && mActivePatternResponse.getPatterns() != null) {
            int lastLookback = mActivePatternResponse.getLookback();
            if (lastLookback <= 0) {
                lastLookback = 10;
            }
            int absoluteStartIndex = Math.max(0, mCandles.size() - lastLookback);

            for (ChartPattern pattern : mActivePatternResponse.getPatterns()) {
                List<PatternDrawingModel.CanvasPoint> translatedPoints = new ArrayList<>();
                for (ChartPattern.Point pt : pattern.getPoints()) {
                    // Resolved index based on unique timestamp (Temporal Stabilization)
                    int resolvedIndex = getIndexByTimestamp(pt.getTimestamp(), pt.getIndex(), absoluteStartIndex);
                    float physicalX = (resolvedIndex * candleWidth) + (candleWidth / 2f) - mOffsetX;
                    float physicalY = getPriceY(pt.getPrice(), minPrice, maxPrice, height);
                    translatedPoints.add(new PatternDrawingModel.CanvasPoint(physicalX, physicalY, pt.getTimestamp()));
                }

                float physicalTargetY = -1;
                if (pattern.getTarget() > 0) {
                    physicalTargetY = getPriceY(pattern.getTarget(), minPrice, maxPrice, height);
                }

                float physicalStopLossY = -1;
                if (pattern.getStopLoss() > 0) {
                    physicalStopLossY = getPriceY(pattern.getStopLoss(), minPrice, maxPrice, height);
                }

                // Map exact Neckline Points if defined mathematically
                List<PatternDrawingModel.CanvasPoint> translatedNeckline = new ArrayList<>();
                if (pattern.getNecklinePoints() != null) {
                    for (ChartPattern.Point pt : pattern.getNecklinePoints()) {
                        int resolvedIndex = getIndexByTimestamp(pt.getTimestamp(), pt.getIndex(), absoluteStartIndex);
                        float physicalX = (resolvedIndex * candleWidth) + (candleWidth / 2f) - mOffsetX;
                        float physicalY = getPriceY(pt.getPrice(), minPrice, maxPrice, height);
                        translatedNeckline.add(new PatternDrawingModel.CanvasPoint(physicalX, physicalY, pt.getTimestamp()));
                    }
                }

                // Map Breakout Point if confirmed
                float breakoutX = -1;
                float breakoutY = -1;
                int resolvedBreakoutIndex = -1;
                if (pattern.getBreakoutTimestamp() > 0) {
                    resolvedBreakoutIndex = getIndexByTimestamp(pattern.getBreakoutTimestamp(), pattern.getBreakoutIndex(), absoluteStartIndex);
                } else if (pattern.getBreakoutIndex() >= 0) {
                    resolvedBreakoutIndex = absoluteStartIndex + pattern.getBreakoutIndex();
                }

                if (resolvedBreakoutIndex >= 0 && resolvedBreakoutIndex < mCandles.size()) {
                    breakoutX = (resolvedBreakoutIndex * candleWidth) + (candleWidth / 2f) - mOffsetX;
                    double relativePrice = (pattern.getBreakoutPrice() - minPrice) / (maxPrice - minPrice);
                    breakoutY = (float) (height - (relativePrice * height));
                }

                // Map Retest Zone bounds
                float retestZoneTopY = -1;
                float retestZoneBottomY = -1;
                if (pattern.getRetestZoneTop() > 0 && pattern.getRetestZoneBottom() > 0) {
                    retestZoneTopY = getPriceY(pattern.getRetestZoneTop(), minPrice, maxPrice, height);
                    retestZoneBottomY = getPriceY(pattern.getRetestZoneBottom(), minPrice, maxPrice, height);
                }

                // Map Projection Area bounds
                float projectionStartX = -1;
                float projectionEndX = -1;
                float projectionTargetY = -1;
                if (pattern.getProjectionStartIndex() >= 0 && pattern.getProjectionEndIndex() >= 0) {
                    int startAbsolute = absoluteStartIndex + pattern.getProjectionStartIndex();
                    int endAbsolute = absoluteStartIndex + pattern.getProjectionEndIndex();
                    projectionStartX = (startAbsolute * candleWidth) + (candleWidth / 2f) - mOffsetX;
                    projectionEndX = (endAbsolute * candleWidth) + (candleWidth / 2f) - mOffsetX;
                    projectionTargetY = getPriceY(pattern.getProjectionTargetPrice(), minPrice, maxPrice, height);
                }

                PatternDrawingModel dynamicModel = new PatternDrawingModel(
                    pattern.getType(),
                    pattern.getConfidence(),
                    pattern.getBias(),
                    pattern.getStartIndex(),
                    pattern.getEndIndex(),
                    translatedPoints,
                    physicalTargetY,
                    physicalStopLossY,
                    pattern.getTarget(),
                    pattern.getStopLoss(),
                    pattern.getExplanation()
                );

                // Set Phase 6 & Phase 3 validation state and structural elements onto drawing model
                dynamicModel.setState(pattern.getState() != null ? pattern.getState() : "STATE_FORMING");
                dynamicModel.setNecklinePoints(translatedNeckline);
                dynamicModel.setBreakoutCoordinates(breakoutX, breakoutY);
                dynamicModel.setRetestZoneCoordinates(retestZoneTopY, retestZoneBottomY);
                dynamicModel.setProjectionCoordinates(projectionStartX, projectionEndX, projectionTargetY);

                PatternRenderer.drawPattern(canvas, dynamicModel);
            }
        }
    }

    /**
     * Helper to lookup exact candlestick index by unique timestamp (Temporal Stabilization)
     */
    private int getIndexByTimestamp(long timestamp, int fallbackIndex, int absoluteStartIndex) {
        if (timestamp > 0 && mCandles != null) {
            for (int i = 0; i < mCandles.size(); i++) {
                if (mCandles.get(i).timestamp == timestamp) {
                    return i;
                }
            }
        }
        return absoluteStartIndex + fallbackIndex; // Fallback to original relative calculation
    }

    private float getPriceY(double price, double minPrice, double maxPrice, int canvasHeight) {
        double relativePrice = (price - minPrice) / (maxPrice - minPrice);
        return (float) (canvasHeight - (relativePrice * canvasHeight));
    }
}
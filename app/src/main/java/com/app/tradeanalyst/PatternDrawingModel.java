package com.tradeanalyst.app;

import java.util.ArrayList;
import java.util.List;

/**
 * RENDER MODEL: PatternDrawingModel
 * Houses the physical canvas pixel coordinates translated from logical indexes and price targets.
 * This object is passed directly to the Canvas drawing renderer.
 */
public class PatternDrawingModel {

    private String type;
    private double confidence;
    private String bias;
    private int startIndex;
    private int endIndex;
    private List<CanvasPoint> points = new ArrayList<>();
    
    private float targetY;
    private float stopLossY;
    private double targetPrice;
    private double stopLossPrice;
    
    private String explanation;

    // =========================================================================
    // CANVAS-TRANSLATED COORDINATES FOR COMPLEX GRAPHICS OVERLAYS (Phase 6)
    // =========================================================================
    private String state = "STATE_FORMING"; // Phase 3 Pattern State Color Tracker
    private List<CanvasPoint> necklinePoints = new ArrayList<>(); // Snapped Neckline
    
    private float breakoutX = -1f; // Breakout Close beacon horizontal index coordinate
    private float breakoutY = -1f; // Breakout Close beacon vertical price coordinate

    private float retestZoneTopY = -1f; // Retest horizontal block top coordinate
    private float retestZoneBottomY = -1f; // Retest horizontal block bottom coordinate

    private float projectionStartX = -1f; // Shaded projection channel start index coordinate
    private float projectionEndX = -1f; // Shaded projection channel end index coordinate
    private float projectionTargetY = -1f; // Shaded projection channel target price coordinate

    /**
     * Represents a coordinate pair mapped directly to screen/canvas pixels.
     */
    public static class CanvasPoint {
        private float x;
        private float y;
        private long timestamp; // Added to support temporal tracking and visual stabilization

        public CanvasPoint() {}

        public CanvasPoint(float x, float y) {
            this.x = x;
            this.y = y;
            this.timestamp = 0L; // Fallback default
        }

        // Overloaded constructor to support exact timestamp snapping
        public CanvasPoint(float x, float y, long timestamp) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public PatternDrawingModel() {}

    public PatternDrawingModel(String type, double confidence, String bias, int startIndex, int endIndex, 
                               List<CanvasPoint> points, float targetY, float stopLossY, 
                               double targetPrice, double stopLossPrice, String explanation) {
        this.type = type;
        this.confidence = confidence;
        this.bias = bias;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.points = points;
        this.targetY = targetY;
        this.stopLossY = stopLossY;
        this.targetPrice = targetPrice;
        this.stopLossPrice = stopLossPrice;
        this.explanation = explanation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getBias() {
        return bias;
    }

    public void setBias(String bias) {
        this.bias = bias;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public List<CanvasPoint> getPoints() {
        return points;
    }

    public void setPoints(List<CanvasPoint> points) {
        this.points = points;
    }

    public float getTargetY() {
        return targetY;
    }

    public void setTargetY(float targetY) {
        this.targetY = targetY;
    }

    public float getStopLossY() {
        return stopLossY;
    }

    public void setStopLossY(float stopLossY) {
        this.stopLossY = stopLossY;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public double getStopLossPrice() {
        return stopLossPrice;
    }

    public void setStopLossPrice(double stopLossPrice) {
        this.stopLossPrice = stopLossPrice;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    // =========================================================================
    // GETTERS, SETTERS, AND HELPER METRIC SETTERS FOR MATH OVERLAYS
    // =========================================================================

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<CanvasPoint> getNecklinePoints() {
        return necklinePoints;
    }

    public void setNecklinePoints(List<CanvasPoint> necklinePoints) {
        this.necklinePoints = necklinePoints;
    }

    public float getBreakoutX() {
        return breakoutX;
    }

    public float getBreakoutY() {
        return breakoutY;
    }

    public void setBreakoutCoordinates(float breakoutX, float breakoutY) {
        this.breakoutX = breakoutX;
        this.breakoutY = breakoutY;
    }

    public float getRetestZoneTopY() {
        return retestZoneTopY;
    }

    public float getRetestZoneBottomY() {
        return retestZoneBottomY;
    }

    public void setRetestZoneCoordinates(float retestZoneTopY, float retestZoneBottomY) {
        this.retestZoneTopY = retestZoneTopY;
        this.retestZoneBottomY = retestZoneBottomY;
    }

    public float getProjectionStartX() {
        return projectionStartX;
    }

    public float getProjectionEndX() {
        return projectionEndX;
    }

    public float getProjectionTargetY() {
        return projectionTargetY;
    }

    public void setProjectionCoordinates(float projectionStartX, float projectionEndX, float projectionTargetY) {
        this.projectionStartX = projectionStartX;
        this.projectionEndX = projectionEndX;
        this.projectionTargetY = projectionTargetY;
    }
}
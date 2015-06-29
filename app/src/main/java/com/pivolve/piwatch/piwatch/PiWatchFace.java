/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pivolve.piwatch.piwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't shown. On
 * devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient mode.
 */
public class PiWatchFace extends CanvasWatchFaceService {
    /**
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        static final int MSG_UPDATE_TIME = 0;

        Paint mBackgroundPaint;
        Paint mDigitalRedPaint;
        Paint mBlackPaint;
        Paint mHandPaint;
        Paint mMinuteHandPaint;
        Paint mSecHandPaint;
        Paint mDigitalPaint;
        Paint mDotPaint;
        Paint mDot1Paint;
        Paint mDot2Paint;
        Paint mDot3Paint;
        Paint mDot2aPaint;
        Paint mDot3aPaint;
        Paint mHighLightPaint;
        Paint mHighLight2Paint;

        Paint mQuotePaint;
        boolean mAmbient;
        Time mTime;
        /**
         * Handler to update the time once a second in interactive mode.
         */
        final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = INTERACTIVE_UPDATE_RATE_MS
                                    - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                        }
                        break;
                }
            }
        };

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mTime.clear(intent.getStringExtra("time-zone"));
                mTime.setToNow();
            }
        };
        boolean mRegisteredTimeZoneReceiver = false;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        String[] quotes = new String[8];

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Typeface myTypeface = Typeface.createFromAsset(getAssets(), "Crysta.ttf");
            setWatchFaceStyle(new WatchFaceStyle.Builder(PiWatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = PiWatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.analog_background));

            mBlackPaint = new Paint();
            mBlackPaint.setColor(resources.getColor(R.color.analog_background));
            mBlackPaint.setAntiAlias(true);

            mHandPaint = new Paint();
            mHandPaint.setColor(resources.getColor(R.color.orange));
            mHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke));
            mHandPaint.setAntiAlias(true);
            mHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinuteHandPaint = new Paint();
            mMinuteHandPaint.setColor(resources.getColor(R.color.orange));
            mMinuteHandPaint.setStrokeWidth(resources.getDimension(R.dimen.analog_hand_stroke_min));
            mMinuteHandPaint.setAntiAlias(true);
            mMinuteHandPaint.setStrokeCap(Paint.Cap.ROUND);

            mSecHandPaint = new Paint();
            mSecHandPaint.setColor(resources.getColor(R.color.blue_1));
            mSecHandPaint.setStrokeWidth(resources.getDimension(R.dimen.medium_small));
            mSecHandPaint.setAntiAlias(true);
            mSecHandPaint.setStrokeCap(Paint.Cap.ROUND);
            mSecHandPaint.setPathEffect(new DashPathEffect(new float[]{1f, 5f}, 0));

            mDotPaint = new Paint();
            mDotPaint.setColor(resources.getColor(R.color.blue_1));
            mDotPaint.setStrokeWidth(resources.getDimension(R.dimen.small));
            mDotPaint.setAntiAlias(true);
            mDotPaint.setStrokeCap(Paint.Cap.ROUND);

            mDot1Paint = new Paint();
            mDot1Paint.setColor(resources.getColor(R.color.blue_1));
            mDot1Paint.setStrokeWidth(resources.getDimension(R.dimen.medium_large));
            mDot1Paint.setAntiAlias(true);
            mDot1Paint.setStrokeCap(Paint.Cap.ROUND);

            mDot2Paint = new Paint();
            mDot2Paint.setColor(resources.getColor(R.color.orange));
            mDot2Paint.setStrokeWidth(resources.getDimension(R.dimen.medium));
            mDot2Paint.setAntiAlias(true);
            mDot2Paint.setStrokeCap(Paint.Cap.SQUARE);

            mDot3Paint = new Paint();
            mDot3Paint.setColor(resources.getColor(R.color.yellow));
            mDot3Paint.setStrokeWidth(resources.getDimension(R.dimen.medium_large));
            mDot3Paint.setAntiAlias(true);
            mDot3Paint.setStrokeCap(Paint.Cap.ROUND);

            mDot2aPaint = new Paint();
            mDot2aPaint.setColor(resources.getColor(R.color.orange));
            mDot2aPaint.setStrokeWidth(resources.getDimension(R.dimen.small));
            mDot2aPaint.setAntiAlias(true);
            mDot2aPaint.setStrokeCap(Paint.Cap.SQUARE);

            mDot3aPaint = new Paint();
            mDot3aPaint.setColor(resources.getColor(R.color.yellow));
            mDot3aPaint.setStrokeWidth(resources.getDimension(R.dimen.medium_small));
            mDot3aPaint.setAntiAlias(true);
            mDot3aPaint.setStrokeCap(Paint.Cap.SQUARE);

            mDigitalPaint = new Paint();
            mDigitalPaint.setColor(resources.getColor(R.color.blue_1));
            mDigitalPaint.setTextSize(32f);
            mDigitalPaint.setTypeface(myTypeface);
            mDigitalPaint.setTextAlign(Paint.Align.CENTER);
            mDigitalPaint.setAntiAlias(true);

            mDigitalRedPaint = new Paint();
            mDigitalRedPaint.setColor(resources.getColor(R.color.orange));
            mDigitalRedPaint.setTextSize(12f);
            mDigitalRedPaint.setTypeface(myTypeface);
            mDigitalRedPaint.setFakeBoldText(true);
            mDigitalRedPaint.setTextAlign(Paint.Align.CENTER);
            mDigitalRedPaint.setAntiAlias(true);

            mHighLightPaint = new Paint();
            mHighLightPaint.setColor(resources.getColor(R.color.red_3));
            mHighLightPaint.setAntiAlias(true);

            mHighLight2Paint = new Paint();
            mHighLight2Paint.setColor(resources.getColor(R.color.yellow));
            mHighLight2Paint.setStrokeWidth(resources.getDimension(R.dimen.medium));
            mHighLight2Paint.setStrokeCap(Paint.Cap.ROUND);
            mHighLight2Paint.setAntiAlias(true);

            mQuotePaint = new Paint();
            mQuotePaint.setColor(resources.getColor(R.color.red_3));
            mQuotePaint.setTextSize(16f);
            mQuotePaint.setTypeface(myTypeface);
            mQuotePaint.setTextAlign(Paint.Align.CENTER);
            mQuotePaint.setAntiAlias(true);

            mTime = new Time();

            quotes[0] = resources.getString(R.string.quote1);
            quotes[1] = resources.getString(R.string.quote2);
            quotes[2] = resources.getString(R.string.quote3);
            quotes[3] = resources.getString(R.string.quote4);
            quotes[4] = resources.getString(R.string.quote5);
            quotes[5] = resources.getString(R.string.quote6);
            quotes[6] = resources.getString(R.string.quote7);
            quotes[7] = resources.getString(R.string.quote8);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        private int mCount;
        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHandPaint.setAntiAlias(!inAmbientMode);
                }
                //mCount++;
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();

            // Draw the background.
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mBackgroundPaint);

            // Find the center. Ignore the window insets so that, on round watches with a
            // "chin", the watch face is centered on the entire screen, not just the usable
            // portion.
            float centerX = width / 2f;
            float centerY = height / 2f;

            float secRot = mTime.second / 30f * (float) Math.PI;
            int minutes = mTime.minute;
            float minRot = minutes / 30f * (float) Math.PI;
            float hrRot = ((mTime.hour + (minutes / 60f)) / 6f) * (float) Math.PI;

            float secLength = centerX - 28;
            float minLength = centerX - 40;
            float hrLength = centerX - 80;

            if (!mAmbient) {
                canvas.drawLines(
                        new float[]{
                                centerX, 5f, centerX, 20f,
                                width - 5f, centerY, width - 20f, centerY,
                                centerX, height - 5, centerX, height - 20f,
                                5f, centerY, 20f, centerY
                        },
                        mDot3Paint);
            } else {
                canvas.drawLines(
                        new float[]{
                                centerX, 5f, centerX, 20f,
                                width - 5f, centerY, width - 20f, centerY,
                                centerX, height - 5, centerX, height - 20f,
                                5f, centerY, 20f, centerY
                        },
                        mDot3aPaint);
            }
            canvas.drawLines(getLines(centerX, centerY, 15f), mDot2aPaint);

            if (!mAmbient) {
                canvas.drawPoints(getPoints(centerX, centerY), mDotPaint);

                float secX = (float) Math.sin(secRot) * secLength;
                float secY = (float) -Math.cos(secRot) * secLength;
                float secX2 = (float) Math.sin(secRot) * 17;
                float secY2 = (float) -Math.cos(secRot) * 17;

                canvas.drawLine(centerX + secX2, centerY + secY2, centerX + secX, centerY + secY, mSecHandPaint);
                if (mTime.second % 15 == 0) {
                    canvas.drawLine(getX(centerX, centerX - 20, secRot), getY(centerY, centerY - 20, secRot),
                            getX(centerX, centerX - 5 , secRot), getY(centerY, centerY - 5, secRot), mDot1Paint);
//                    canvas.drawCircle(getX(centerX, centerX - 10, secRot), getY(centerY, centerX - 10, secRot), 10f, mHighLightPaint);
//                    canvas.drawCircle(getX(centerX, centerX - 10, secRot), getY(centerY, centerX - 10, secRot), 5f, mDigitalRedPaint);
                } else if (mTime.second % 5 == 0) {
                    canvas.drawLine(getX(centerX, centerX - 15, secRot), getY(centerY, centerY - 15, secRot),
                            getX(centerX, centerX - 5, secRot), getY(centerY, centerY - 5, secRot), mDot1Paint);

                } else {
                    //canvas.drawCircle(getX(centerX, centerX - 10, secRot), getY(centerY, centerX - 10, secRot), 10f, mHighLightPaint);
                    canvas.drawCircle(getX(centerX, centerX - 10, secRot), getY(centerY, centerX - 10, secRot), 3f, mDotPaint);
                }

                canvas.drawText(mTime.format("%m-%d"), centerX, centerY - 67, mDigitalRedPaint);
                canvas.drawText(mTime.format("%H:%M"), centerX, centerY - 36, mDigitalPaint);
                int pick = mCount % 4;
                canvas.drawText(quotes[pick], centerX, centerY - 18, mQuotePaint);
            } else {
                mCount++;
            }


            float minX = (float) Math.sin(minRot) * minLength;
            float minY = (float) -Math.cos(minRot) * minLength;
            float minX2 = (float) Math.sin(minRot) * 17;
            float minY2 = (float) -Math.cos(minRot) * 17;
            canvas.drawLine(centerX + minX2, centerY + minY2, centerX + minX, centerY + minY, mMinuteHandPaint);

            float hrX = (float) Math.sin(hrRot) * hrLength;
            float hrY = (float) -Math.cos(hrRot) * hrLength;
            float hrX2 = (float) Math.sin(hrRot) * 17;
            float hrY2 = (float) -Math.cos(hrRot) * 17;
            canvas.drawLine(centerX + hrX2, centerY + hrY2, centerX + hrX, centerY + hrY, mHandPaint);

            canvas.drawCircle(centerX, centerY, 11f, mHandPaint);
            canvas.drawCircle(centerX, centerY, 8f, mBlackPaint);
        }
        private float[] getLines(float centerX, float centerY, float size) {
            return new float[]{
                    centerX + (float) (centerX * Math.sin(Math.PI / 6)), centerY - (float) (centerX * Math.cos(Math.PI / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI / 3)), centerY - (float) (centerX * Math.cos(Math.PI / 3)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI / 3)), centerY - (float) ((centerX - size) * Math.cos(Math.PI / 3)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 4 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 4 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 4 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 4 / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 5 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 5 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 5 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 5 / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 7 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 7 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 7 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 7 / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 8 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 8 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 8 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 8 / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 10 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 10 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 10 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 10 / 6)),
                    centerX + (float) (centerX * Math.sin(Math.PI * 11 / 6)), centerY - (float) (centerX * Math.cos(Math.PI * 11 / 6)),
                    centerX + (float) ((centerX - size) * Math.sin(Math.PI * 11 / 6)), centerY - (float) ((centerX - size) * Math.cos(Math.PI * 11 / 6))
            };
        }
        private float getX(float centerX, float length, float angle) {
            return centerX + (float) (length * Math.sin(angle));
        }
        private float getY(float centerY, float length, float angle) {
            return centerY - (float) (length * Math.cos(angle));
        }
        private float[] getPoints(float centerX, float centerY) {
            return new float[] {
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 1 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 1 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 2 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 2 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 3 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 3 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 4 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 4 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 6 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 6 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 7 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 7 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 8 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 8 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 9 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 9 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 11 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 11 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 12 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 12 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 13 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 13 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 14 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 14 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 16 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 16 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 17 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 17 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 18 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 18 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 19 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 19 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 21 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 21 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 22 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 22 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 23 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 23 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 24 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 24 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 26 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 26 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 27 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 27 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 28 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 28 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 29 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 29 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 26 / 30)), centerY - (float) ((centerX - 10)* Math.cos(Math.PI * 26 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 27 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 27 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 28 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 28 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 29 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 29 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 31 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 31 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 32 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 32 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 33 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 33 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 34 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 34 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 36 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 36 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 37 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 37 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 38 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 38 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 39 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 39 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 41 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 41 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 42 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 42 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 43 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 43 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 44 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 44 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 46 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 46 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 47 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 47 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 48 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 48 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 49 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 49 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 51 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 51 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 52 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 52 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 53 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 53 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 54 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 54 / 30)),

                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 56 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 56 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 57 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 57 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 58 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 58 / 30)),
                    centerX + (float) ((centerX - 10) * Math.sin(Math.PI * 59 / 30)), centerY - (float) ((centerX - 10) * Math.cos(Math.PI * 59 / 30))

            };

        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mTime.clear(TimeZone.getDefault().getID());
                mTime.setToNow();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            PiWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            PiWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }
    }
}

package com.gashfara.imagewatchface1;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.graphics.Matrix;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.graphics.Color;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;


public class MyWatchService extends CanvasWatchFaceService {

    private final static String TAG = MyWatchService.class.getSimpleName();



     // provide your watch face implementation
     // CanvasWatchFaceService.Engineを継承したクラスを返す

    @Override
    public Engine onCreateEngine() {
        Engine en = new Engine();
        return en;
    }

     //implement service callback methods
    private class Engine extends CanvasWatchFaceService.Engine {
        float HOUR_HAND_LENGTH = 80.0f;
        final float MINUTE_HAND_LENGTH = 120.0f;
        final int[] BACKGROUND_RES_ID = {
                R.drawable.image_0,
        };

        Paint mHourPaint;
        Paint mMinutePaint;
         Paint mTimePaint;
        Bitmap mBackgroundBitmap;
        Bitmap mBackgroundScaledBitmap;

        Time mTime;

         // initialize
        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            Log.d(TAG, "onCreate");
            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            mHourPaint = new Paint();
            mHourPaint.setStrokeWidth(5f);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setStrokeCap(Paint.Cap.ROUND);

            mMinutePaint = new Paint();
            mMinutePaint.setStrokeWidth(3f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.ROUND);

            mTimePaint = new Paint();
            mTimePaint.setAntiAlias(true);

            mTime = new Time();


        }

         //毎分実行される
        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }


         // 盤面を描写する
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            mTime.setToNow();

            int width = bounds.width();
            int height = bounds.height();
            float centerX = width / 2f;
            float centerY = height / 2f;

            // draw background
            // 背景の描写
            Resources resources = MyWatchService.this.getResources();
            int imgResId;
            imgResId = BACKGROUND_RES_ID[mTime.minute % BACKGROUND_RES_ID.length];
            Drawable backgroundDrawable = resources.getDrawable(imgResId);
            //転送画像があればそれを表示する
            //if(mGlobals.gBackgroundDrawable != null) {
            //    backgroundDrawable = mGlobals.gBackgroundDrawable;
            //}
            SharedPreferences pref =getSharedPreferences("prefImage",MODE_PRIVATE);
            String prefstr = pref.getString("bitmap", "");
            if(prefstr != ""){
                byte[] bytes = Base64.decode(prefstr.getBytes(),Base64.DEFAULT);
                Bitmap bitmap =  BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                backgroundDrawable = new BitmapDrawable(getResources(), bitmap);
            }

            mBackgroundBitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
            //引き伸ばし
            //mBackgroundScaledBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
            //        width, height, true );
            //縦横比そのままで画面に合わせる。
            int w = mBackgroundBitmap.getWidth();
            int h = mBackgroundBitmap.getHeight();
            float scale = Math.max((float)width/w, (float)height/h);
            int size = Math.min(w, h);
            Matrix matrix = new Matrix();
            matrix.postScale(scale, scale);
            mBackgroundScaledBitmap= Bitmap.createBitmap(mBackgroundBitmap, (w-size)/2, (h-size)/2, size, size, matrix, true);
            //ToDo mBackgroundBitmapを開放
            //描画
            canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);

            // draw minute hand
            float minRot = mTime.minute / 30f * (float) Math.PI;
            mMinutePaint.setARGB(255, 255, 255, 255);

            float minX = (float) Math.sin(minRot) * MINUTE_HAND_LENGTH;
            float minY = (float) -Math.cos(minRot) * MINUTE_HAND_LENGTH;
            canvas.drawLine(centerX, centerY, centerX + minX, centerY + minY, mMinutePaint);

            // draw hour hand
            float hrRot = ((mTime.hour + (mTime.minute / 60f)) / 6f) * (float) Math.PI;
                //mHourPaint.setARGB(255, 255, 0, 0);
            mHourPaint.setARGB(255, 255, 215, 0);
            float hrX = (float) Math.sin(hrRot) * HOUR_HAND_LENGTH;
            float hrY = (float) -Math.cos(hrRot) * HOUR_HAND_LENGTH;
            canvas.drawLine(centerX, centerY, centerX + hrX, centerY + hrY, mHourPaint);

            //時刻を表示 縁あり
            DateFormat df = new SimpleDateFormat("HH:mm");
            Date date = new Date(System.currentTimeMillis());
            mTimePaint.setTextSize(15);
            mTimePaint.setStrokeWidth(2.0f);
            mTimePaint.setColor(Color.BLACK);
            mTimePaint.setStyle(Paint.Style.STROKE);
            canvas.drawText(df.format(date), 50, 50, mTimePaint);
            mTimePaint.setStrokeWidth(0);
            mTimePaint.setColor(Color.WHITE);
            mTimePaint.setStyle(Paint.Style.FILL);
            canvas.drawText(df.format(date), 50, 50, mTimePaint);

        }

    }


}

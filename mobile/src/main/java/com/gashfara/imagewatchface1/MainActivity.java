package com.gashfara.imagewatchface1;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.io.InputStream;
import android.graphics.BitmapFactory;
import android.widget.Toast;
import java.util.Locale;
import java.util.Random;
import android.content.res.Configuration;
import android.content.res.Resources;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import android.os.Build;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {


    //private WebView mWebView;

    //mogi add for send photo //
    private static final String TAG = "MainActivity";

    /** Request code for launching the Intent to resolve Google Play services errors. */
    private static final int REQUEST_RESOLVE_ERROR = 1000;

    private static final String COUNT_PATH = "/count";
    private static final String IMAGE_PATH = "/imagefacewatch";
    private static final String IMAGE_KEY = "photo";
    //private static final String COUNT_KEY = "count";

    private GoogleApiClient mGoogleApiClient;
    private boolean mResolvingError = false;

    //private ListView mDataItemList;
    private Button mTakePhotoBtn;
    private Button mSendPhotoBtn;
    private ImageView mThumbView;
    private Bitmap mImageBitmap;
    private Activity mActivity;
    private WebView mWebView;


    // Send DataItems.
    //private ScheduledExecutorService mGeneratorExecutor;
    //private ScheduledFuture<?> mDataItemGeneratorFuture;

    private static final int REQUEST_KITKAT_PICK_CONTENT = 1000;
    private static final int REQUEST_PICK_CONTENT = 1001;


    //mogi end//
    //admob関連
    // 広告ID
    private String UnitID ;
    // テスト用の端末番号
    private String MobileID = "407C0D3B4DDF68A9A20BCBF9E1D36AD0";
    private InterstitialAd mInterstitialAd;
    // インタースティシャル用のView
    private View mViewAd = null;
    private AdView adView = null;

    boolean mIsEnglishTest = false;//英語版テストフラグ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*英語版に切り替え　英語でのテスト用*/
        if(mIsEnglishTest) {
            Locale locale = Locale.getDefault();            // アプリで使用されているロケール情報を取得
            locale = Locale.US;
            Locale.setDefault(locale);                      // 新しいロケールを設定
            Configuration config = new Configuration();
            config.locale = locale;                         // Resourcesに対するロケールを設定
            Resources resources = getBaseContext().getResources();
            resources.updateConfiguration(config, null);    // Resourcesに対する新しいロケールを反映
            LOGD(TAG, "onCreateLocale" + getString(R.string.app_name));
            LOGD(TAG, "onCreateLocale" + getString(R.string.take_photo));
        }

        // mogi add for send photo //
        //mHandler = new Handler();
        LOGD(TAG, "onCreate");
        setContentView(R.layout.main_activity);
        setupViews();

        //admob作成　全面広告
        //UnitIDセット
        UnitID = getString(R.string.UnitID);
        // インタースティシャルを作成する。
        mInterstitialAd = new InterstitialAd(getBaseContext());
        mInterstitialAd.setAdUnitId(UnitID);
        // Set the AdListener.
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                String message = String.format("onAdFailedToLoad (%s)", getErrorReason(errorCode));
                LOGD(TAG, "onCreateAdLoad:"+message);
            }
        });

        //admob バナーを作成
        adView = (AdView)this.findViewById(R.id.adView);
        // 本番モード
        AdRequest adRequest = new AdRequest.Builder().build();
        // テストモード
        /*
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)    // エミュレータ
                .addTestDevice(MobileID) // テストデバイス
                .build();
        */
        adView.loadAd(adRequest);

        //data layer関連
        mActivity = this;
        //mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        //接続
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

    }
    //mogi add fro send photo //
    //カメラの画像を得る。
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            try {
                //画像を得る
                InputStream is = getContentResolver().openInputStream(data.getData());
                //リサイズする
                // inJustDecodeBounds=true で画像のサイズをチェック
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(is, null,options);
                is.close();
                is = getContentResolver().openInputStream(data.getData());
                // inSampleSize を計算
                options.inSampleSize = calculateInSampleSize(options, 320, 320);
                // inSampleSize をセットしてデコード
                options.inJustDecodeBounds = false;
                mImageBitmap =  BitmapFactory.decodeStream(is,null, options);
                //mImageBitmap = BitmapFactory.decodeStream(is);
                is.close();
                mThumbView.setImageBitmap(mImageBitmap);
            }catch(Exception e) {
                LOGD(TAG, e.getMessage());
            }

            //画像送信 できない。closeされてる
            /*
            LOGD(TAG, "posting image");
            if (null != mImageBitmap && mGoogleApiClient.isConnected()) {
                LOGD(TAG, "posting image start");
                sendPhoto(toAsset(mImageBitmap));
            }
            */
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        //mWebView.loadUrl("http://google.com");
        //mogi add for send photo//
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }

    }

    //mogi add for send photo //
    //Data Items
    @Override
    public void onResume() {
        super.onResume();
        /*
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
        */
    }
    //mogi add for send photo //
    //Data Items
    @Override
    public void onPause() {
        super.onPause();
        //mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
    }


    @Override
    protected void onStop() {
        //mogi add for send photo //
        if (!mResolvingError) {
            mGoogleApiClient.disconnect();
        }
        //mogi end//
        super.onStop();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }





    // mogi add for send photo//
    //Data Items
    @Override //ConnectionCallbacks
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "Google API Client was connected");
        mResolvingError = false;
    }

    //Data Items
    @Override //ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "Connection to Google API client was suspended");
        mSendPhotoBtn.setEnabled(false);
    }

    //Data Items
    @Override //OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            Log.e(TAG, "Connection to Google API client has failed");
            mResolvingError = false;
            mSendPhotoBtn.setEnabled(false);
        }
    }






    /** Generates a DataItem based on an incrementing count. */
    /*まったくいらない？
    private class DataItemGenerator implements Runnable {

        private int count = 0;

        @Override
        public void run() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(COUNT_PATH);
            putDataMapRequest.getDataMap().putInt(COUNT_KEY, count++);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            LOGD(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                        + dataItemResult.getStatus().getStatusCode());
                            }
                        }
                    });
        }
    }
    */

    /**
     * Dispatches an {@link android.content.Intent} to take a photo. Result will be returned back
     * in onActivityResult().
     */
    private void dispatchTakePictureIntent() {
        //カメラの場合
        //Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        //    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        //}
        //ギャラリー 4.3と4.4以降でちがう
        Intent takePictureIntent;
        int requestInt;
        if (Build.VERSION.SDK_INT < 19){
            takePictureIntent = new Intent(Intent.ACTION_PICK);
            requestInt = REQUEST_PICK_CONTENT;
        } else {
            takePictureIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            requestInt = REQUEST_KITKAT_PICK_CONTENT;
        }

        takePictureIntent.setType("image/*");
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, requestInt);
        }
    }

    /**
     * Builds an {@link com.google.android.gms.wearable.Asset} from a bitmap. The image that we get
     * back from the camera in "data" is a thumbnail size. Typically, your image should not exceed
     * 320x320 and if you want to have zoom and parallax effect in your app, limit the size of your
     * image to 640x400. Resize your image before transferring to your wearable device.
     */
    private static Asset toAsset(Bitmap bitmap) {
        ByteArrayOutputStream byteStream = null;
        try {
            byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } finally {
            if (null != byteStream) {
                try {
                    byteStream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Sends the asset that was created form the photo we took by adding it to the Data Item store.
     */
    private void sendPhoto(Asset asset) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(IMAGE_PATH);
        dataMap.getDataMap().putAsset(IMAGE_KEY, asset);
        dataMap.getDataMap().putLong("time", new Date().getTime());
        PutDataRequest request = dataMap.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        LOGD(TAG, "Sending image was successful: " + dataItemResult.getStatus()
                                .isSuccess());
                        //admob表示
                        loadInterstitial(mViewAd);
                        showInterstitial(mViewAd,2);
                        //トースト表示
                        Toast.makeText(mActivity, getString(R.string.send_success), Toast.LENGTH_LONG).show();
                    }
                });

    }

    public void onTakePhotoClick(View view) {
        dispatchTakePictureIntent();
    }

    public void onSendPhotoClick(View view) {
        if (null != mImageBitmap && mGoogleApiClient.isConnected()) {
            sendPhoto(toAsset(mImageBitmap));
        }
    }

    /**
     * Sets up UI components and their callback handlers.
     */
    private void setupViews() {
        mTakePhotoBtn = (Button) findViewById(R.id.takePhoto);
        mSendPhotoBtn = (Button) findViewById(R.id.sendPhoto);

        // Shows the image received from the handset
        mThumbView = (ImageView) findViewById(R.id.imageView);

        mWebView = (WebView) findViewById(R.id.webView);
        Locale locale = Locale.getDefault();            // アプリで使用されているロケール情報を取得
        if (locale.equals(Locale.JAPAN)){
            mWebView.loadUrl( "file:///android_asset/index.html" );
        }else {
            mWebView.loadUrl( "file:///android_asset/index-en.html" );
        }

    }

    /**
     * As simple wrapper around Log.d
     */
    private static void LOGD(final String tag, String message) {
        //if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        //}
    }
    //mogi end//
    //縮小サイズを計算
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // 画像の元サイズ
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            if (width > height) {
                inSampleSize = Math.round((float)height / (float)reqHeight);
            } else {
                inSampleSize = Math.round((float)width / (float)reqWidth);
            }
        }
        return inSampleSize;
    }
    //admob関連
    // Called when the Load Interstitial button is clicked.
    public void loadInterstitial(View unusedView) {
        // 広告リクエストを作成する (本番)
        AdRequest adRequest = new AdRequest.Builder().build();

        //広告Test用
        /*
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice(MobileID)
                .build();
        */
        // Load the interstitial ad.
        mInterstitialAd.loadAd(adRequest);
    }

    // Called when the Show Interstitial button is clicked.
    public void showInterstitial(View unusedView, int interval) {
        // ランダムに表示させる場合
        if(interval !=0 ){
            Random r = new Random();
            int rand = r.nextInt(interval);
            if(rand==0){
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("", "Interstitial ad was not ready to be shown...... ");
                }
            }
        }
    }
    // Gets a string error reason from an error code.
    private String getErrorReason(int errorCode) {
        String errorReason = "";
        switch(errorCode) {
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                errorReason = "ERROR_CODE_INTERNAL_ERROR";
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
                errorReason = "ERROR_CODE_INVALID_REQUEST";
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                errorReason = "ERROR_CODE_NETWORK_ERROR";
                break;
            case AdRequest.ERROR_CODE_NO_FILL:
                errorReason = "ERROR_CODE_NO_FILL";
                break;
        }
        return errorReason;
    }

}

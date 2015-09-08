
package com.gashfara.imagewatchface1;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import android.content.SharedPreferences;


/**
 * Listens to DataItems and Messages from the local node.
 */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayerListenerServic";

    public static final String IMAGE_PATH = "/imagefacewatch";
    public static final String IMAGE_KEY = "photo";
    GoogleApiClient mGoogleApiClient;
    private Handler mHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler = new Handler();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }


    @Override
    public void onPeerConnected(Node peer) {
        LOGD(TAG, "onPeerConnected: " + peer);
    }

    @Override
    public void onPeerDisconnected(Node peer) {
        LOGD(TAG, "onPeerDisconnected: " + peer);
    }

    public static void LOGD(final String tag, String message) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message);
        }
    }
    //データ受信
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photo = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Setting background image..");
                            BitmapDrawable bitDraw = new BitmapDrawable(getResources(), bitmap);
                            //グローバルに画像をセットする
                            //mGlobals.gBackgroundDrawable = bitDraw;
                            //Prefに画像をセット
                            SharedPreferences pref = getSharedPreferences("prefImage", MODE_PRIVATE);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                            String bitmapString = Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT);
                            pref.edit().putString("bitmap",bitmapString).commit();

                        }
                    });

                }
            }
        }
    }

    /**
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                apiClient, asset).await().getInputStream();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        return BitmapFactory.decodeStream(assetInputStream);
    }

}

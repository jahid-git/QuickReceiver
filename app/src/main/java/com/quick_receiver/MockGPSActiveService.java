package com.quick_receiver;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;

public class MockGPSActiveService extends Service {
    private static final int DELAY_MILLISECONDS = 0;
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MockGPSActiveService";
    public static Activity activity;
    private Handler handler;
    private Runnable runnable;
    private LocationManager locationManager;
    private NotificationManager notificationManager;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        handler = new Handler();

        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Location location = new Location("gps");

                    location.setLatitude(MapsActivity.latitude);
                    location.setLongitude(MapsActivity.longitude);

                    location.setAccuracy(15.0F);
                    location.setAltitude(0.0);
                    location.setBearing(0.0F);
                    location.setTime(System.currentTimeMillis());
                    location.setSpeed(1.2f);
                    location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());
                    Bundle bundle = new Bundle();
                    bundle.putInt("satellites", 7);
                    location.setExtras(bundle);

                    locationManager.setTestProviderLocation(LocationManager.NETWORK_PROVIDER, location);
                    sendServiceStatus(true);
                    handler.postDelayed(runnable, DELAY_MILLISECONDS);
                } catch (Exception e) {
                    sendServiceStatus(false);
                    stopForeground(true);
                    stopSelf();
                    //showOpenDeveloperOptionsDialog();
                }
            }
        };
    }

    @SuppressLint({"InvalidWakeLockTag", "WakelockTimeout", "InlinedApi", "WrongConstant"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if ("STOP_ACTION".equals(intent.getAction())) {
                stopForeground(true);
                stopSelf();
                return START_STICKY;
            }
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.NETWORK_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, false);
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {}


        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
            } else {
                locationManager.addTestProvider(LocationManager.GPS_PROVIDER, false, true, false,
                        false, true, true, true, Criteria.POWER_HIGH, Criteria.ACCURACY_FINE);
            }
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
            }
        } catch (Exception e) {
        }

        /*try {
            locationManager.addTestProvider(LocationManager.NETWORK_PROVIDER, false, false,
                    false, false, false, false,
                    false, ProviderProperties.POWER_USAGE_HIGH, ProviderProperties.ACCURACY_FINE);
        } catch (Exception e) {}
         */

        handler.postDelayed(runnable, DELAY_MILLISECONDS);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "Mock GPS Active Service Channel", NotificationManager.IMPORTANCE_HIGH);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(mChannel);
            }

            Intent notifyIntent = new Intent(this, MapsActivity.class);
            notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_IMMUTABLE);

            Intent stopIntent = new Intent(activity, MockGPSActiveService.class);
            stopIntent.setAction("STOP_ACTION");
            PendingIntent stopPendingIntent = PendingIntent.getService(activity, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

            notification = new Notification.Builder(this)
                    .setChannelId(CHANNEL_ID)
                    .setContentTitle("Running...")
                    .setContentText(MapsActivity.address)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setShowWhen(false)
                    .addAction(R.drawable.ic_stop, "Stop", stopPendingIntent)
                    .build();
        }


        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeLockTag");
        wakeLock.acquire();

        startForeground(NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendServiceStatus(false);
        handler.removeCallbacks(runnable);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        try {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.NETWORK_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.removeTestProvider(LocationManager.GPS_PROVIDER);
            }
        } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void showOpenDeveloperOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enable Developer Options");
        builder.setMessage("To use advanced features, please enable Developer Options on your device. "
                + "Do you want to open Developer Options now?");
        builder.setCancelable(false);
        builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openDeveloperOptionsSettings();
            }
        });

        builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        });

        builder.show();
    }

    private void openDeveloperOptionsSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            activity.startActivity(intent);
        } catch (Exception e){
            showEnableDeveloperOptionsDialog();
        }
    }

    private void showEnableDeveloperOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enable Developer Options");
        builder.setMessage("To use advanced features, please enable Developer Options on your device. "
                + "Go to Settings -> About phone -> Tap 'Build number' seven times.");
        builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void sendServiceStatus(boolean status) {
        Intent intent = new Intent(MapsActivity.MockGPSServiceReceiver.ACTION_SERVICE_STATUS);
        intent.putExtra(MapsActivity.MockGPSServiceReceiver.EXTRA_SERVICE_STATUS, status);
        sendBroadcast(intent);
    }
}
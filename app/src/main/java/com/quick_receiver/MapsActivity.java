package com.quick_receiver;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.quick_receiver.databinding.ActivityMapsBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int SPEECH_REQUEST_CODE = 123;
    private MockGPSServiceReceiver receiver;
    public static boolean serviceStatus;
    public static GoogleMap googleMap;
    private Circle outerCircle;
    private Circle innerCircle;
    private Marker marker;
    private FusedLocationProviderClient fusedLocationClient;
    private ImageView searchButton;
    private ImageView microphoneButton;
    private AppCompatEditText searchEditText;
    private MaterialButton startStopButton;
    private ImageButton favoriteButton;
    private ImageButton saveButton;
    //private ImageButton settingsButton;
    private ImageButton zoomInButton;
    private ImageButton currentLocationButton;
    private ImageButton zoomOutButton;
    public static Dialog favoriteDialog;
    private InputMethodManager imm;
    public static double latitude = 25.204842259930164;
    public static double longitude = 55.27111038565636;
    public static String address = "Sheikh Zayed Rd - Dubai - United Arab Emirates";

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PrefsUtilities.init(this);

        MockGPSActiveService.activity = this;
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if(!PrefsUtilities.getPrefs(PrefsUtilities.FIRST_TIME, false)) {
            showPasswordDialog();
            PrefsUtilities.setPrefs(PrefsUtilities.FIRST_TIME, true);
        }

        ActivityMapsBinding binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        searchButton = findViewById(R.id.search_btn);
        microphoneButton = findViewById(R.id.microphone_btn);
        searchEditText = findViewById(R.id.search_view);
        startStopButton = findViewById(R.id.start_stop_btn);
        favoriteButton = findViewById(R.id.favorite_btn);
        saveButton = findViewById(R.id.save_btn);
        //settingsButton = findViewById(R.id.settings_btn);
        zoomInButton = findViewById(R.id.zoom_in_btn);
        currentLocationButton = findViewById(R.id.current_location_btn);
        zoomOutButton = findViewById(R.id.zoom_out_btn);

        searchButton.setOnClickListener(this);
        microphoneButton.setOnClickListener(this);
        startStopButton.setOnClickListener(this);
        favoriteButton.setOnClickListener(this);
        saveButton.setOnClickListener(this);
        //settingsButton.setOnClickListener(this);
        zoomInButton.setOnClickListener(this);
        currentLocationButton.setOnClickListener(this);
        zoomOutButton.setOnClickListener(this);

        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (keyEvent != null && keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    searchLocation(textView.getText().toString());
                    return true;
                }
                return false;
            }
        });

        receiver = new MockGPSServiceReceiver();
        IntentFilter filter = new IntentFilter(MockGPSServiceReceiver.ACTION_SERVICE_STATUS);
        registerReceiver(receiver, filter);
    }

    public class MockGPSServiceReceiver extends BroadcastReceiver {
        public static final String ACTION_SERVICE_STATUS = "com.quick_receiver.SERVICE_STATUS";
        public static final String EXTRA_SERVICE_STATUS = "service_status";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null && intent.getAction().equals(ACTION_SERVICE_STATUS)) {
                serviceStatus = intent.getBooleanExtra(EXTRA_SERVICE_STATUS, false);
                if(serviceStatus){
                    startStopButton.setText("Stop");
                    startStopButton.setIconResource(R.drawable.ic_stop);
                    startStopButton.setIconTintResource(android.R.color.holo_red_dark);
                } else {
                    startStopButton.setText("Start");
                    startStopButton.setIconResource(R.drawable.ic_play);
                    startStopButton.setIconTintResource(android.R.color.holo_green_dark);
                }
            }
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        showExitDialog();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;

        LatLng defaultLatLng = new LatLng(latitude, longitude);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 16f));
        googleMap.addMarker(new MarkerOptions().position(defaultLatLng).title(address));
        addCircle(defaultLatLng, 80);

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                MapsActivity.latitude = latLng.latitude;
                MapsActivity.longitude = latLng.longitude;

                googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));

                if (marker != null) {
                    marker.remove();
                }

                Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses != null && addresses.size() > 0) {
                        Address address = addresses.get(0);
                        String addressText = address.getAddressLine(0);
                        MapsActivity.address = addressText;
                        marker = googleMap.addMarker(new MarkerOptions().position(latLng).title(addressText));
                    }
                } catch (IOException e) {
                    showToast(e.getMessage());
                }
            }
        });

        //currentLocation();
    }

    @Override
    public void onClick(View view) {
        int viewId = view.getId();
        if (viewId == R.id.search_btn) {
            String searchText = searchEditText.getText().toString().trim();
            if (!searchText.isEmpty()) {
                searchLocation(searchText);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }
            } else {
                showToast("Please enter a search query");
            }
        } else if (viewId == R.id.microphone_btn) {
            try {
                Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                startActivityForResult(speechIntent, SPEECH_REQUEST_CODE);
            } catch (ActivityNotFoundException e) {
                showToast("Speech recognition not available on this device");
            }
        } else if (viewId == R.id.start_stop_btn) {
            Intent mockGpsActiveService = new Intent(MapsActivity.this, MockGPSActiveService.class);
            if(MapsActivity.serviceStatus) {
                stopService(mockGpsActiveService);
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(mockGpsActiveService);
                } else {
                    startService(new Intent(mockGpsActiveService));
                }
            }
        } else if (viewId == R.id.save_btn) {
            showSaveDialog();
        } else if (viewId == R.id.favorite_btn) {
            showFavoriteDialog();
        } /*else if (viewId == R.id.settings_btn) {
            showSettingsDialog();
        } */ else if (viewId == R.id.zoom_in_btn) {
            googleMap.animateCamera(CameraUpdateFactory.zoomIn());
        } else if (viewId == R.id.current_location_btn) {
            currentLocation();
        } else if (viewId == R.id.zoom_out_btn) {
            googleMap.animateCamera(CameraUpdateFactory.zoomOut());
        }
    }

    private void searchLocation(String query) {
        if (marker != null) {
            marker.remove();
        }
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && addresses.size() > 0) {
                Address address = addresses.get(0);
                LatLng location = new LatLng(address.getLatitude(), address.getLongitude());
                MapsActivity.latitude = location.latitude;
                MapsActivity.longitude = location.longitude;
                String addressText = address.getAddressLine(0);
                MapsActivity.address = addressText;
                marker = googleMap.addMarker(new MarkerOptions().position(location).title(addressText));
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f));
                searchEditText.setText("");
            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException ignored) {}
    }
    private void addCircle(LatLng center, double radius) {

        if (innerCircle != null) {
            innerCircle.remove();
        }
        if (outerCircle != null) {
            outerCircle.remove();
        }

        CircleOptions outerCircleOptions = new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(1)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(30, 0, 0, 255));

        CircleOptions innerCircleOptions = new CircleOptions()
                .center(center)
                .radius(3)
                .strokeWidth(1)
                .strokeColor(Color.BLUE)
                .fillColor(Color.argb(80, 0, 0, 255));

            outerCircle = googleMap.addCircle(outerCircleOptions);
            innerCircle = googleMap.addCircle(innerCircleOptions);
    }

    private void currentLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            googleMap.clear();
                            MapsActivity.latitude = location.getLatitude();
                            MapsActivity.longitude = location.getLongitude();

                            LatLng currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 18f));
                            addCircle(currentLocation, 80);
                            Geocoder currentLocationGeocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
                            try {
                                List<Address> addresses = currentLocationGeocoder.getFromLocation(currentLocation.latitude, currentLocation.longitude, 1);
                                if (addresses != null && addresses.size() > 0) {
                                    Address address = addresses.get(0);
                                    String addressText = address.getAddressLine(0);
                                    MapsActivity.address = addressText;
                                    marker = googleMap.addMarker(new MarkerOptions().position(currentLocation).title(addressText));
                                }
                            } catch (IOException e) {
                                showToast(e.getMessage());
                            }
                        }
                    });

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void showPasswordDialog(){
        final Dialog passwordDialog = new Dialog(this);
        passwordDialog.setCancelable(false);
        passwordDialog.setContentView(R.layout.password_dialog);
        EditText editText = (EditText) passwordDialog.findViewById(R.id.password_dialog_edittext);
        editText.requestFocus();

        Button goButton = passwordDialog.findViewById(R.id.password_dialog_go_btn);
        goButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = editText.getText().toString().trim();
                if(!password.isEmpty() && password.equalsIgnoreCase("8920")){
                    passwordDialog.dismiss();
                } else {
                    showToast("Please enter valid password!");
                }
            }
        });
        passwordDialog.show();
    }

    private void showSaveDialog(){

        final Dialog saveDialog = new Dialog(this);
        saveDialog.setContentView(R.layout.save_dialog);
        EditText editText = (EditText) saveDialog.findViewById(R.id.save_dialog_editText);
        editText.setText(address);
        editText.selectAll();
        editText.requestFocus();

        TextView textView = (TextView) saveDialog.findViewById(R.id.save_dialog_textview);
        textView.setText("Latitute: " + latitude + "\nLongitude: " + longitude);

        String key = "(" + latitude + " , " + longitude + ")";

        Button saveButton = saveDialog.findViewById(R.id.save_dialog_save_btn);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String address = editText.getText().toString().trim();
                if(!address.isEmpty()){
                    PrefsUtilities.setPrefs(key, address);
                    saveDialog.dismiss();
                } else {
                    showToast("Enter a valid address");
                }

            }
        });
        saveDialog.show();
    }

    private void showFavoriteDialog(){
        MapsActivity.favoriteDialog = new Dialog(this);
        favoriteDialog.setContentView(R.layout.favorite_dialog);

        LocationListAdapter keyValueAdapter = new LocationListAdapter(this, PrefsUtilities.getAllLocations());
        ListView keyValueListView = favoriteDialog.findViewById(R.id.location_listview);
        keyValueListView.setAdapter(keyValueAdapter);

        Button closeButton = favoriteDialog.findViewById(R.id.favorite_dialog_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                favoriteDialog.dismiss();
            }
        });
        favoriteDialog.show();
    }


    private void showSettingsDialog(){
        final Dialog aboutDialog = new Dialog(this);
        aboutDialog.setContentView(R.layout.settings_dialog);
        Button closeButton = aboutDialog.findViewById(R.id.about_dialog_close_btn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aboutDialog.dismiss();
            }
        });
        aboutDialog.show();
    }

    private void showExitDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exit!");
        builder.setMessage("Do you want to exit the app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String recognizedSpeech = matches.get(0);
                searchEditText.setText(recognizedSpeech);
                searchEditText.setSelection(recognizedSpeech.length());
                searchLocation(recognizedSpeech);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(googleMap);
            }
        }
    }
}

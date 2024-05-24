package com.quick_receiver;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Map;

public class LocationListAdapter extends ArrayAdapter<String> {

    public LocationListAdapter(Context context, List<String> locationList) {
        super(context, R.layout.location_list_item, locationList);
    }

    @Override
    public View getView(int position, View v, ViewGroup parent) {
        String locationInfo = getItem(position);
        String[] parts = locationInfo.split(":");
        String latLon = parts[0];
        String address = parts[1];
        ViewHolder holder;

        @SuppressLint("ViewHolder")
        final View convertView = LayoutInflater.from(getContext()).inflate(R.layout.location_list_item, parent, false);

        holder = new ViewHolder();

        holder.addressTextView = convertView.findViewById(R.id.address);
        holder.latLonTextView = convertView.findViewById(R.id.lanLog);
        holder.deleteButton = convertView.findViewById(R.id.delete_btn);

        View item = convertView.findViewById(R.id.location_list_item);

        convertView.setTag(holder);

        holder.addressTextView.setText(address);
        holder.latLonTextView.setText(latLon);

        holder.deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remove(locationInfo);
                notifyDataSetChanged();
                PrefsUtilities.removePrefs(latLon);
            }
        });

        item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] loc = latLon.replace("(", "").replace(")", "").replace(" ", "").split(",");
                MapsActivity.latitude = Double.parseDouble(loc[0]);
                MapsActivity.longitude = Double.parseDouble(loc[1]);

                LatLng defaultLatLng = new LatLng(MapsActivity.latitude, MapsActivity.longitude);
                MapsActivity.googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 18f));
                MapsActivity.googleMap.addMarker(new MarkerOptions().position(defaultLatLng).title(address));

                Intent mockGpsActiveService = new Intent(v.getContext(), MockGPSActiveService.class);
                if(MapsActivity.serviceStatus) {
                    v.getContext().stopService(mockGpsActiveService);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.getContext().startForegroundService(mockGpsActiveService);
                } else {
                    v.getContext().startService(new Intent(mockGpsActiveService));
                }
                MapsActivity.favoriteDialog.dismiss();
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView addressTextView;
        TextView latLonTextView;
        ImageView deleteButton;
    }
}

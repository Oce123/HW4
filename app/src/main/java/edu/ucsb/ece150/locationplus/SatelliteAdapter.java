package edu.ucsb.ece150.locationplus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SatelliteAdapter extends RecyclerView.Adapter<SatelliteAdapter.ViewHolder> {

    private List<Satellite> satelliteList;

    // Constructor to initialize the satelliteList
    public SatelliteAdapter(List<Satellite> satelliteList) {
        this.satelliteList = satelliteList;
    }

    // Setter method to update the satelliteList
    public void setSatelliteList(List<Satellite> satelliteList) {
        this.satelliteList = satelliteList;
    }

    // ViewHolder class to hold the views for each item in the RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView satelliteInfoTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            satelliteInfoTextView = itemView.findViewById(R.id.satelliteInfoTextView);
        }

        public void bind(Satellite satellite) {
            satelliteInfoTextView.setText(
                    "Type: " + satellite.getSatelliteType() +
                            ", PRN: " + satellite.getSatellitePrn() +
                            ", C/N0: " + satellite.getSatelliteCn0() +
                            ", Azimuth: " + satellite.getAzimuth() +
                            ", Elevation: " + satellite.getElevation() +
                            ", Carrier Frequency: " + satellite.getCarrierFrequency() +
                            ", C/N0: " + satellite.getCarrierNoiseDensity() +
                            ", Constellation: " + satellite.getConstellationName() +
                            ", SVID:" + satellite.getSvid()
            );
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the RecyclerView
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_satellite, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // Bind the data to the views for each item
        Satellite satellite = satelliteList.get(position);
        holder.satelliteInfoTextView.setText(satellite.toString());
    }

    @Override
    public int getItemCount() {
        return satelliteList != null ? satelliteList.size() : 0;
    }
}

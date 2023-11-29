package edu.ucsb.ece150.locationplus;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

/*
 * This class is provided as a way for you to store information about a single satellite. It can
 * be helpful if you would like to maintain the list of satellites using an ArrayList (i.e.
 * ArrayList<Satellite>). As in Homework 3, you can then use an Adapter to update the list easily.
 *
 * You are not required to implement this if you want to handle satellite information in using
 * another method.
 */
public class Satellite {

    // [TODO] Define private member variables
    private int satelliteType;
    private int satellitePrn;
    private float satelliteCn0;
    private float azimuth;
    private float elevation;
    private float carrierFrequency;
    private float carrierNoiseDensity;
    private String constellationName;
    private int svid;
    private Location location;

    // [TODO] Write the constructor
    public Satellite(int type, int prn, float cn0, float azimuth, float elevation,
                     float carrierFrequency, float carrierNoiseDensity, String constellationName, int svid, Location location) {
        this.satelliteType = type;
        this.satellitePrn = prn;
        this.satelliteCn0 = cn0;
        this.azimuth = azimuth;
        this.elevation = elevation;
        this.carrierFrequency = carrierFrequency;
        this.carrierNoiseDensity = carrierNoiseDensity;
        this.constellationName = constellationName;
        this.svid = svid;
        this.location = location;
    }

    public int getSatelliteType() {
        return satelliteType;
    }

    public int getSatellitePrn() {
        return satellitePrn;
    }

    public float getSatelliteCn0() {
        return satelliteCn0;
    }
    public float getAzimuth() {
        return azimuth;
    }

    public float getElevation() {
        return elevation;
    }

    public float getCarrierFrequency() {
        return carrierFrequency;
    }

    public float getCarrierNoiseDensity() {
        return carrierNoiseDensity;
    }

    public String getConstellationName() {
        return constellationName;
    }
    public int getSvid() {
        return svid;
    }


    // [TODO] Implement the toString() method. When the Adapter tries to assign names to items
    // in the ListView, it calls the toString() method of the objects in the ArrayList
    @Override
    public String toString() {
        return "Number: " + satelliteType +
                ", Azimuth: " + azimuth + "°" +
                ", Elevation: " + elevation + "°" +
                ", Carrier Frequency: " + carrierFrequency + "Hz" +
                ", Carrier-Noise Density C/N0: " + carrierNoiseDensity + "dB Hz" +
                ", Constellation Name: " + constellationName +
                ", SVID: " + svid + "\n";
    }

    public Location getLocation() {
        return location;
    }
}

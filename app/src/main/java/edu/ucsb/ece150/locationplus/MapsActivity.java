package edu.ucsb.ece150.locationplus;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends AppCompatActivity implements LocationListener, OnMapReadyCallback {
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 100;
    private Geofence mGeofence;
    private GeofencingClient mGeofencingClient;
    private PendingIntent mPendingIntent = null;
    private static final String KEY_LAST_KNOWN_LATITUDE = "last_known_latitude";
    private static final String KEY_LAST_KNOWN_LONGITUDE = "last_known_longitude";
    private GnssStatus.Callback mGnssStatusCallback;
    private GoogleMap mMap;
    private LocationManager mLocationManager;
    private Toolbar mToolbar;
    private boolean autoCenteringEnabled = false;
    private LatLng lastKnownLatLng;
    private boolean satelliteInfoVisible = false;
    private List<Satellite> satelliteList = new ArrayList<>();
    private GnssStatus lastGnssStatus;
    private SatelliteAdapter satelliteAdapter;
    private ToggleButton satelliteButton;
    private RelativeLayout satellitesPopupContainer;
    private RecyclerView recyclerViewSatellites;
    private TextView satelliteInfoTextView;
    private Location userLocation;
    private LatLng geofenceCenter;
    private boolean geofenceAdded = false;
    private Button cancelGeofenceButton;
    private Marker mGeofenceMarker;


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        //setContentView(R.layout.item_satellite);

        // Set up Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Set up Geofencing Client
        mGeofencingClient = LocationServices.getGeofencingClient(MapsActivity.this);

        // Set up Satellite List
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // [TODO] Ensure that necessary permissions are granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);

            // [TODO] Implement behavior when the satellite status is updated
            mGnssStatusCallback = new GnssStatus.Callback() {
                @Override
                public void onSatelliteStatusChanged(GnssStatus status) {
                    // Implement behavior when the satellite status is updated
                    // For example, update a list or adapter with satellite information.
                    handleSatelliteStatus(status);
                }
            };
            mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }

        // [TODO] Additional setup for viewing satellite information (lists, adapters, etc.)

        // Set up Toolbar
        mToolbar = (Toolbar) findViewById(R.id.appToolbar);
        setSupportActionBar(mToolbar);

        // Initialize your views
        satelliteButton = findViewById(R.id.satelliteButton);
        satellitesPopupContainer = findViewById(R.id.satellitesPopupContainer);
        recyclerViewSatellites = findViewById(R.id.recyclerViewSatellites);

        // Initialize your adapter and set it to the RecyclerView
        if (recyclerViewSatellites != null) {
            // Initialize your adapter and set it to the RecyclerView
            recyclerViewSatellites.setLayoutManager(new LinearLayoutManager(this));
            satelliteAdapter = new SatelliteAdapter(satelliteList);
            recyclerViewSatellites.setAdapter(satelliteAdapter);
        } else {
            Log.e("RecyclerView", "RecyclerView is null");
        }

        findViewById(R.id.toggleAutoCenter).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleAutoCentering();
            }
        });

        // Set click listener for the satelliteButton
        satelliteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle visibility of the satellitesPopupContainer
                if (satellitesPopupContainer.getVisibility() == View.VISIBLE) {
                    satellitesPopupContainer.setVisibility(View.INVISIBLE);
                    satelliteInfoTextView.setVisibility(View.INVISIBLE); // Hide the TextView
                } else {
                    satellitesPopupContainer.setVisibility(View.VISIBLE);
                    showSatellitesPopup();
                    satelliteInfoTextView.setVisibility(View.VISIBLE); // Show the TextView
                }
            }
        });
        // Initialize satelliteInfoTextView
        satelliteInfoTextView = findViewById(R.id.satelliteInfoTextView);

        cancelGeofenceButton = findViewById(R.id.cancelGeofenceButton);
        cancelGeofenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Appel à une méthode pour annuler le Geofence
                cancelGeofence();
            }
        });

    }

    private void handleSatelliteStatus(GnssStatus status) {
        satelliteList.clear(); // Clear actual list

        int satelliteCount = status.getSatelliteCount();

        for (int i = 0; i < satelliteCount; i++) {
            int satelliteType = status.getConstellationType(i);
            int satellitePrn = status.getSvid(i);
            float satelliteCn0 = status.getCn0DbHz(i);
            float azimuth = status.getAzimuthDegrees(i);
            float elevation = status.getElevationDegrees(i);
            float carrierFrequency = status.getCarrierFrequencyHz(i);
            float carrierNoiseDensity = status.getCn0DbHz(i);
            String constellationName = getConstellationName();
            int svid = status.getSvid(i);

            Location satelliteLocation = new Location("satellite");
            satelliteLocation.setLatitude(0.0);
            satelliteLocation.setLongitude(0.0);

            Satellite satellite = new Satellite(satelliteType, satellitePrn, satelliteCn0,
                    azimuth, elevation, carrierFrequency, carrierNoiseDensity, constellationName, svid, satelliteLocation);
            satelliteList.add(satellite);
        }

        // Update the last value of GnssStatus
        lastGnssStatus = status;

        // Set text for satelliteInfoTextView
        updateSatelliteInfoUI();
    }

    public String getConstellationName() {
        if (userLocation != null && lastGnssStatus != null && !satelliteList.isEmpty()) {
            Satellite satellite = satelliteList.get(0); // Utilisez le satellite approprié selon votre logique

            // Calculer la distance entre la position de l'utilisateur et la position du satellite
            float distance = userLocation.distanceTo(satellite.getLocation());

            if (distance < 100) {
                return "GPS";
            } else {
                return "Unknown";
            }
        } else {
            return "Unknown";
        }
    }


    // Méthode pour mettre à jour l'interface utilisateur avec les informations satellites
    private void updateSatelliteInfoUI() {
        // [TODO] Mettez à jour votre interface utilisateur avec les informations satellites
        TextView satelliteInfoTextView = findViewById(R.id.satelliteInfoTextView);

        List<String> satelliteInfoList = new ArrayList<>();

        for (Satellite satellite : satelliteList) {
            String satelliteInfo = satellite.toString();
            satelliteInfoList.add(satelliteInfo);
        }

        satelliteInfoTextView.setText(TextUtils.join("\n", satelliteInfoList));

        satelliteAdapter.setSatelliteList(satelliteList);
        satelliteAdapter.notifyDataSetChanged();
    }


    // Méthode pour obtenir la liste mise à jour des satellites
    public List<Satellite> getSatelliteList() {
        return satelliteList;
    }

    private void toggleSatelliteInfoPopup() {
        // Toggle visibility of the satellitesPopupContainer
        if (satellitesPopupContainer.getVisibility() == View.VISIBLE) {
            satellitesPopupContainer.setVisibility(View.INVISIBLE);
            satelliteButton.setChecked(false);
        } else {
            satellitesPopupContainer.setVisibility(View.VISIBLE);
            showSatellitesPopup();
            satelliteButton.setChecked(true);
        }
    }

    private void showSatellitesPopup() {
        // [TODO] Update the list of satellites in the adapter
        // Assuming you have a method getSatelliteList() to get the list of satellites
        List<Satellite> updatedSatellites = getSatelliteList();
        satelliteAdapter.setSatelliteList(updatedSatellites);
        satelliteAdapter.notifyDataSetChanged();

        // Set text for satelliteInfoTextView
        updateSatelliteInfoUI();
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // [TODO] Implement behavior when Google Maps is ready

        // Ajouter un marqueur à la position actuelle de l'utilisateur
        if (lastKnownLatLng != null) {
            mMap.addMarker(new MarkerOptions().position(lastKnownLatLng).title("Ma Position"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(lastKnownLatLng));
        }else{
            // Ajouter un marqueur à Santa Barbara
            LatLng santaBarbara = new LatLng(34.4208, -119.6982);
            mMap.addMarker(new MarkerOptions().position(santaBarbara).title("Santa Barbara"));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(santaBarbara));
        }

        // [TODO] In addition, add a listener for long clicks (which is the starting point for
        // creating a Geofence for the destination and listening for transitions that indicate
        // arrival)
        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                // [TODO] Handle the long click event
                // This is the starting point for creating a Geofence for the destination
                // and listening for transitions that indicate arrival
                handleMapLongClick(latLng);
            }
        });
    }

    private void handleMapLongClick(LatLng latLng) {
        // Vérifie si un geofence existe déjà
        if (geofenceAdded) {
            // Affiche un message à l'utilisateur indiquant qu'il y a déjà un geofence
            Toast.makeText(this, "Un geofence existe déjà", Toast.LENGTH_SHORT).show();
            return;
        }

        // Vérifie si la permission de localisation est accordée
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission accordée, crée la boîte de dialogue de confirmation
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Voulez-vous définir cet emplacement comme destination?")
                    .setPositiveButton("Oui", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // L'utilisateur a cliqué sur "Oui", crée le Geofence
                            createGeofence(latLng);
                            dialog.dismiss(); // Ferme la boîte de dialogue
                        }
                    })
                    .setNegativeButton("Non", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // L'utilisateur a cliqué sur "Non", ne fait rien
                            dialog.dismiss(); // Ferme la boîte de dialogue
                        }
                    });

            // Affiche la boîte de dialogue
            builder.create().show();

        } else {
            // La permission de localisation n'est pas accordée, demande la permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
    }


    private void createGeofence(LatLng latLng) {
        // [TODO] Create a Geofence for the destination
        geofenceCenter = latLng;  // Stocke les coordonnées du centre du geofence
        mGeofence = new Geofence.Builder()
                .setRequestId("Destination Geofence") // Replace with a unique ID
                .setCircularRegion(latLng.latitude, latLng.longitude, 100) // 100 meters radius
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();

        // [TODO] Add the Geofence to the GeofencingClient
        addGeofence();
    }

    private void addGeofence() {
        // Vérifie si un geofence existe déjà
        if (geofenceAdded) {
            // Affiche un message à l'utilisateur indiquant qu'il y a déjà un geofence
            Toast.makeText(this, "Un geofence existe déjà", Toast.LENGTH_SHORT).show();
            return;
        }
        // Check for location permission before adding the Geofence
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mGeofencingClient.addGeofences(getGeofenceRequest(), getGeofencePendingIntent())
                    .addOnSuccessListener(this, task -> {
                        // Geofences added successfully
                        // [TODO] Handle success if needed
                        Toast.makeText(MapsActivity.this, "Geofences added successfully", Toast.LENGTH_SHORT).show();
                        // Ajoute un marqueur à l'emplacement du geofence
                        if (geofenceCenter != null) {
                            MarkerOptions markerOptions = new MarkerOptions()
                                    .position(geofenceCenter)
                                    .title("Emplacement du Geofence")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)); // Changer la couleur du marqueur
                            mGeofenceMarker = mMap.addMarker(markerOptions);
                        }
                    })
                    .addOnFailureListener(this, e -> {
                        // Failed to add Geofences
                        // [TODO] Handle failure if needed
                        Toast.makeText(MapsActivity.this, "Failed to add geofences", Toast.LENGTH_SHORT).show();
                        Log.e("Geofence", "Failed to add geofences", e);
                    });
        } else {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSIONS_REQUEST_LOCATION);
        }
        geofenceAdded = true;
    }

    private void cancelGeofence() {
        // Vérifiez si un Geofence est actuellement en cours
        if (geofenceAdded) {
            // Supprimez le Geofence
            mGeofencingClient.removeGeofences(getGeofencePendingIntent())
                    .addOnSuccessListener(this, task -> {
                        // Geofence supprimé avec succès
                        // [TODO] Gérez le succès si nécessaire
                        Toast.makeText(MapsActivity.this, "Geofence annulé avec succès", Toast.LENGTH_SHORT).show();
                        // Effacez le marqueur de l'emplacement du Geofence
                        if (geofenceCenter != null) {
                            mGeofenceMarker.remove();
                            mGeofenceMarker = null;
                        }
                        geofenceAdded = false;
                    })
                    .addOnFailureListener(this, e -> {
                        // Échec de la suppression du Geofence
                        // [TODO] Gérez l'échec si nécessaire
                        Toast.makeText(MapsActivity.this, "Échec de l'annulation du Geofence", Toast.LENGTH_SHORT).show();
                        Log.e("Geofence", "Échec de l'annulation du Geofence", e);
                    });
        } else {
            // Aucun Geofence en cours, affichez un message à l'utilisateur
            Toast.makeText(this, "Aucun Geofence en cours", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // [TODO] Implement behavior when a location update is received
        // Mise à jour de la dernière position connue
        lastKnownLatLng = new LatLng(location.getLatitude(), location.getLongitude());

        // [TODO] Implementer le comportement lorsque la mise à jour de la position est reçue
        // Afficher un marqueur à la nouvelle position
        if (autoCenteringEnabled) {
            mMap.clear(); // Effacez les marqueurs précédents
            mMap.addMarker(new MarkerOptions().position(lastKnownLatLng).title("Ma Position"));
            mMap.animateCamera(CameraUpdateFactory.newLatLng(lastKnownLatLng));
        }
        userLocation = location;
    }

    private void toggleAutoCentering() {
        autoCenteringEnabled = !autoCenteringEnabled;

        if (autoCenteringEnabled && lastKnownLatLng != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(lastKnownLatLng));
        }
    }

    /*
     * The following three methods onProviderDisabled(), onProviderEnabled(), and onStatusChanged()
     * do not need to be implemented -- they must be here because this Activity implements
     * LocationListener.
     *
     * You may use them if you need to.
     */
    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private GeofencingRequest getGeofenceRequest() {
        // [TODO] Set the initial trigger (i.e. what should be triggered if the user is already
        // inside the Geofence when it is created)

        return new GeofencingRequest.Builder()
                //.setInitialTrigger()  <--  Add triggers here
                .addGeofence(mGeofence)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mPendingIntent != null) {
            return mPendingIntent;
        }

        Intent intent = new Intent(MapsActivity.this, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(
                MapsActivity.this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE // Add this line
        );

        return mPendingIntent;
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStart() throws SecurityException {
        super.onStart();

        // [TODO] Ensure that necessary permissions are granted (look in AndroidManifest.xml to
        // see what permissions are needed for this app)

        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
        mLocationManager.registerGnssStatusCallback(mGnssStatusCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // [TODO] Data recovery
        // Récupérer la dernière position de l'utilisateur depuis SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        double lastKnownLatitude = preferences.getFloat(KEY_LAST_KNOWN_LATITUDE, 0.0f);
        double lastKnownLongitude = preferences.getFloat(KEY_LAST_KNOWN_LONGITUDE, 0.0f);

        // Mettre à jour la dernière position connue
        if (lastKnownLatitude != 0.0 && lastKnownLongitude != 0.0) {
            lastKnownLatLng = new LatLng(lastKnownLatitude, lastKnownLongitude);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // [TODO] Data saving
        SharedPreferences.Editor editor = null;
        if (lastKnownLatLng != null) {
            // Utiliser SharedPreferences.Editor pour enregistrer les données
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            editor = preferences.edit();

            // Enregistrer la dernière latitude et longitude
            editor.putFloat(KEY_LAST_KNOWN_LATITUDE, (float) lastKnownLatLng.latitude);
            editor.putFloat(KEY_LAST_KNOWN_LONGITUDE, (float) lastKnownLatLng.longitude);

            editor.apply();
        }
        editor.apply();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onStop() {
        super.onStop();

        mLocationManager.removeUpdates(this);
        mLocationManager.unregisterGnssStatusCallback(mGnssStatusCallback);
    }
}

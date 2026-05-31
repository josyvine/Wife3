package com.inout.app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import org.osmdroid.config.Configuration;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Full Screen Dialog for selecting a location on a map.
 * Uses OpenStreetMap (osmdroid).
 * Features: Stationary center pointer, map panning, and text search.
 */
public class MapSelectionDialog extends DialogFragment {

    private MapView mapView;
    private EditText etSearch;
    private CardView cvSearchBar;
    private OnLocationSelectedListener listener;

    /**
     * Interface to pass the captured coordinates and address back to the Admin fragment.
     */
    public interface OnLocationSelectedListener {
        void onLocationSelected(double lat, double lng, String addressName);
    }

    public void setOnLocationSelectedListener(OnLocationSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // OSMDroid configuration initialization
        Context ctx = requireContext().getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        
        // Use Full Screen Theme
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_map_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind UI Elements
        mapView = view.findViewById(R.id.map_view);
        etSearch = view.findViewById(R.id.et_map_search);
        cvSearchBar = view.findViewById(R.id.cv_search_bar);
        
        ImageButton btnClose = view.findViewById(R.id.btn_close_map);
        ImageButton btnToggleSearch = view.findViewById(R.id.btn_toggle_search);
        Button btnSearchGo = view.findViewById(R.id.btn_map_search_go);
        ImageButton btnMyLocation = view.findViewById(R.id.btn_my_location);
        ImageButton btnZoomIn = view.findViewById(R.id.btn_zoom_in);
        ImageButton btnZoomOut = view.findViewById(R.id.btn_zoom_out);
        Button btnSave = view.findViewById(R.id.btn_confirm_selection);

        setupMap();
        centerOnCurrentLocation();

        // Close the dialog
        btnClose.setOnClickListener(v -> dismiss());

        // Toggle search visibility
        btnToggleSearch.setOnClickListener(v -> {
            if (cvSearchBar.getVisibility() == View.VISIBLE) {
                cvSearchBar.setVisibility(View.GONE);
                hideKeyboard();
            } else {
                cvSearchBar.setVisibility(View.VISIBLE);
                etSearch.requestFocus();
                showKeyboard();
            }
        });

        // Trigger search
        btnSearchGo.setOnClickListener(v -> performSearch());
        
        // Trigger search on keyboard action
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });

        // New: GPS recenter button action
        btnMyLocation.setOnClickListener(v -> centerOnCurrentLocation());

        // Manual zoom controls
        btnZoomIn.setOnClickListener(v -> mapView.getController().zoomIn());
        btnZoomOut.setOnClickListener(v -> mapView.getController().zoomOut());

        // Capture logic
        btnSave.setOnClickListener(v -> confirmSelection());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK); // Standard OSM tiles
        mapView.setMultiTouchControls(true); // Pinch to zoom support
        
        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);
    }

    private void centerOnCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                == PackageManager.PERMISSION_GRANTED) {
            
            LocationManager lm = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
            Location lastLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            
            if (lastLocation == null) {
                lastLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (lastLocation != null) {
                GeoPoint startPoint = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
                mapView.getController().setCenter(startPoint);
            } else {
                // Fallback coordinates if GPS hasn't locked yet
                mapView.getController().setCenter(new GeoPoint(0.0, 0.0));
                Toast.makeText(getContext(), "GPS Signal weak. Please use Search.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void performSearch() {
        String query = etSearch.getText().toString().trim();
        if (TextUtils.isEmpty(query)) return;

        hideKeyboard();
        
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address result = addresses.get(0);
                GeoPoint target = new GeoPoint(result.getLatitude(), result.getLongitude());
                
                mapView.getController().animateTo(target);
                mapView.getController().setZoom(17.0); 
            } else {
                Toast.makeText(getContext(), "Address not found.", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Toast.makeText(getContext(), "Geocoder Error. Check connection.", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmSelection() {
        // Capture map center (exactly under the stationary pointer)
        GeoPoint centerPoint = (GeoPoint) mapView.getMapCenter();
        double lat = centerPoint.getLatitude();
        double lng = centerPoint.getLongitude();

        StringBuilder addressBuilder = new StringBuilder();
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                
                // 1. Get Place Name / Brand / Feature
                String feature = addr.getFeatureName();
                // Logic: If FeatureName is a Plus Code (contains +), ignore it
                if (feature != null && !feature.contains("+")) {
                    addressBuilder.append(feature).append(", ");
                }

                // 2. Get Street / Thoroughfare
                if (addr.getThoroughfare() != null) {
                    addressBuilder.append(addr.getThoroughfare()).append(", ");
                }

                // 3. Get Locality / Area / Neighborhood
                String area = addr.getSubLocality() != null ? addr.getSubLocality() : addr.getLocality();
                if (area != null) {
                    addressBuilder.append(area).append(" ");
                }

                // 4. Get Postal Code (Explicitly requested)
                if (addr.getPostalCode() != null) {
                    addressBuilder.append("- ").append(addr.getPostalCode());
                }
            }
        } catch (IOException e) {
            // If reverse geocoding fails, fallback to generic name
        }

        String finalAddress = addressBuilder.toString().trim();
        // Clean up trailing commas
        if (finalAddress.endsWith(",")) {
            finalAddress = finalAddress.substring(0, finalAddress.length() - 1);
        }

        if (TextUtils.isEmpty(finalAddress)) {
            finalAddress = "Map Point Location";
        }

        if (listener != null) {
            listener.onLocationSelected(lat, lng, finalAddress);
        }
        dismiss();
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private void hideKeyboard() {
        View view = getView();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
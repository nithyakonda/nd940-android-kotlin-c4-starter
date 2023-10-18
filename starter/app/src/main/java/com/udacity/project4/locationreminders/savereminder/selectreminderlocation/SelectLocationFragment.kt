package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    private val runningQOrLater = android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.Q
    private var isZoomed = false
    companion object {
        private const val TAG =  "SelectLocationFragment"
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private lateinit var backgroundPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var foregroundPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        foregroundPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.d(TAG, "Show snack bar from foregroundPermissionLauncher")
                showPermissionDeniedSnackBar()
            } else {
                // TODO Check if location permissions are granted and if so enable the
                // location data layer.
                Log.d(TAG, "Calling enableMyLocation from activity result")
                enableMyLocation()
                zoomToLastLocation()
            }
        }

        if (runningQOrLater) {
            backgroundPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Log.d(TAG, "Show snack bar from backgroundPermissionLauncer")
                    showPermissionDeniedSnackBar()
                } else {
                    enableMyLocation()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val layoutId = R.layout.fragment_select_location
        binding = DataBindingUtil.inflate(inflater, layoutId, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        return binding.root
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d(TAG, "Calling enableMyLocation from onMapReady")
        enableMyLocation()
        zoomToLastLocation()
        setMapStyle(map)
        setMapClicks(map)

        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()
    }

    /** Map Manipulation functions - Begin **/
    private fun setMapClicks(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            val marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
            )
            promptForLocationName(marker)
        }

        map.setOnPoiClickListener{poi ->
            val marker = map.addMarker(MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
            )
            marker?.showInfoWindow()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (foregroundLocationPermissionApproved()) {
            map.isMyLocationEnabled = true
        } else {
            requestForegroundPermission()
        }
    }

    @SuppressLint("MissingPermission")
    private fun zoomToLastLocation() {
        val zoomLevel = 15f
        val location =
            LocationServices.getFusedLocationProviderClient(requireActivity()).lastLocation
        location.addOnSuccessListener {
            it?.let {
                val home = LatLng(it.latitude, it.longitude)
                map.addMarker(MarkerOptions().position(home).title("Home"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, zoomLevel))
                isZoomed = true
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            // Customize the styling of the base map using a JSON object defined
            // in a raw resource file.
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        }catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    /** Map Manipulation functions - End **/

    /** Permission Util Functions - Begin **/

    private fun requestBackgroundPermission() {
        if (runningQOrLater) {
            Log.d(TAG, "requestBackgroundPermission")
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun requestForegroundPermission() {
        Log.d(TAG, "requestForegroundPermission")
        foregroundPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }


    private fun foregroundLocationPermissionApproved(): Boolean = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION) &&
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(requireContext(),
                            Manifest.permission.ACCESS_COARSE_LOCATION))
    @TargetApi(29)
    private fun backgroundPermissionApproved()  {
        if (runningQOrLater) {
            PackageManager.PERMISSION_GRANTED ==
                    ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
        } else {
            true
        }
    }

    /** Permission Util Functions - End **/





    /** UI Utils - Begin **/


    private fun promptForLocationName(marker: Marker?) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.enter_location_name))

        val input = EditText(context)
        builder.setView(input)

        builder.setPositiveButton("OK") { dialog, which ->
            marker?.title = input.text?.toString()
            marker?.showInfoWindow()
        }
        builder.create().show()
    }
    private fun showPermissionDeniedSnackBar() {
        Snackbar.make(
            binding.mapsContainer,
            R.string.permission_denied_explanation,
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(R.string.settings) {
                startActivity(Intent().apply {
                    action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            }.show()
    }
    /** UI Utils - End **/

    private fun onLocationSelected() {
        // TODO: When the user confirms on the selected location,
        //  send back the selected location details to the view model
        //  and navigate back to the previous fragment to save the reminder and add the geofence
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        // TODO: Change the map type based on the user's selection.
        R.id.normal_map -> {
            true
        }
        R.id.hybrid_map -> {
            true
        }
        R.id.satellite_map -> {
            true
        }
        R.id.terrain_map -> {
            true
        }
        else -> super.onOptionsItemSelected(item)
    }
}
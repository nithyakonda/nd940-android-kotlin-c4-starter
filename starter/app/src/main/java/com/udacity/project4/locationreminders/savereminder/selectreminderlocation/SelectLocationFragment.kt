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
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragmentDirections
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.foregroundLocationPermissionApproved
import com.udacity.project4.utils.requestForegroundPermission
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

//    private var isZoomed = false
    companion object {
        private const val TAG =  "SelectLocationFragment"
    }

    // Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap
    private var selectedLocation: Marker? = null
    private var selectedPoi:PointOfInterest? = null

    private lateinit var foregroundPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        foregroundPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Log.d(TAG, "Show snack bar from foregroundPermissionLauncher")
                showPermissionDeniedSnackBar()
            } else {
                Log.d(TAG, "Calling enableMyLocation from activity result")
                enableMyLocation()
                zoomToLastLocation()
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

        binding.saveLocation.setOnClickListener {
            onLocationSelected()
        }
        return binding.root
    } override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        Log.d(TAG, "Calling enableMyLocation from onMapReady")
        enableMyLocation()
        zoomToLastLocation()
        setMapStyle(map)
        setMapClicks(map)


    }


    private fun onLocationSelected() {
        _viewModel.longitude.value = selectedLocation?.position?.longitude
        _viewModel.latitude.value = selectedLocation?.position?.latitude
        _viewModel.reminderSelectedLocationStr.value = selectedLocation?.title
        _viewModel.selectedPOI.value = selectedPoi

        val directions = SelectLocationFragmentDirections.actionSelectLocationFragmentToSaveReminderFragment()
        _viewModel.navigationCommand.value = NavigationCommand.To(directions)

        Log.i(TAG, """ Selected location
            |long: ${_viewModel.longitude.value}
            |lat: ${_viewModel.latitude.value}
            |location: ${_viewModel.reminderSelectedLocationStr.value}
            |poi: ${_viewModel.selectedPOI.value?.placeId.toString()}
        """.trimMargin() )
    }

    /** Map Manipulation functions - Begin **/
    private fun setMapClicks(map: GoogleMap) {
        map.setOnMapLongClickListener { latLng ->
            selectedLocation = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
            )
            promptForLocationName(selectedLocation)
        }

        map.setOnPoiClickListener{poi ->
            selectedPoi = poi
            selectedLocation = map.addMarker(MarkerOptions()
                .position(poi.latLng)
                .title(poi.name)
            )
            selectedLocation?.showInfoWindow()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (foregroundLocationPermissionApproved(requireContext())) {
            map.isMyLocationEnabled = true
        } else {
            requestForegroundPermission(foregroundPermissionLauncher)
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
                selectedLocation = map.addMarker(MarkerOptions().position(home).title("Home"))
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(home, zoomLevel))
//                isZoomed = true
            }
        }
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
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
}
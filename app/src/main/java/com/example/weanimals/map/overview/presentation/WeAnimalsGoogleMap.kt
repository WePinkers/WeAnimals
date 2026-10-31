package com.example.weanimals.map.overview.presentation

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class WeAnimalsGoogleMap @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), OnMapReadyCallback {

    private val mapView: MapView = MapView(context)
    private var googleMap: GoogleMap? = null
    private var onMapReadyListener: ((GoogleMap) -> Unit)? = null

    init {
        addView(mapView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mapView.getMapAsync(this)
    }

    fun getMapView(): MapView = mapView

    fun getGoogleMap(): GoogleMap? = googleMap

    fun setOnMapReadyListener(listener: (GoogleMap) -> Unit) {
        onMapReadyListener = listener
        googleMap?.let { listener(it) }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        onMapReadyListener?.invoke(map)
    }

    fun centerOnCoordinates(latitude: Double, longitude: Double, zoom: Float = 14f) {
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(latitude, longitude), zoom)
        )
    }

    fun addMarker(latitude: Double, longitude: Double, title: String?, snippet: String?) {
        googleMap?.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .title(title)
                .snippet(snippet)
        )
    }
}

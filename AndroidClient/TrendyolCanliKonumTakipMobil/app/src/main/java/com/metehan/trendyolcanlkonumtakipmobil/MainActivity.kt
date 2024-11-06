package com.metehan.trendyolcanlkonumtakipmobil


/*
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.metehan.trendyolcanlkonumtakipmobil.ui.theme.TrendyolCanliKonumTakipMobilTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.Response
import okio.ByteString
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.UUID
import java.util.concurrent.TimeUnit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import org.osmdroid.views.overlay.Marker

// Bu ilk yaptığım butona basınca konum gönderen kısım
class MainActivity : ComponentActivity() {

    private lateinit var webSocket: WebSocket
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val receivedMessages = mutableStateOf("")
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            getLastKnownLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrendyolCanliKonumTakipMobilTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SimpleButton(
                            onClick = { checkLocationPermission() },
                            messages = receivedMessages.value
                        )
                    }
                }
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initiateWebSocket()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getLastKnownLocation()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendLocationMessage(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Konum alınamadı", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Konum alma hatası: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // İzinler uygun değilse yeniden izin isteği yapılabilir
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun sendLocationMessage(lat: Double, lng: Double) {
        val message = mapOf("type" to "location", "lat" to lat, "lng" to lng)
        val jsonMessage = Gson().toJson(message)
        webSocket.send(jsonMessage)
    }

    private fun initiateWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            //.url("ws://10.0.2.2:8080/ws/mobile")
            .url("ws://192.168.1.33:8080/ws/mobile" )
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Connected!")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived: $text"
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived bytes: ${bytes.utf8()}"
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                runOnUiThread {
                    Log.d("Test123", "WebSocket Closing: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Error: ${t.message}")
                }
            }
        }

        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }
}

@Composable
fun SimpleButton(onClick: () -> Unit, messages: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            onClick()
            Toast.makeText(context, "Message Sent!", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    ) {
        Text(text = "Send Location")
    }
    Text(text = messages)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrendyolCanliKonumTakipMobilTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleButton(onClick = {}, messages = "")
        }
    }
}
*/


/*
class MainActivity : ComponentActivity() {

    private val uuid = UUID.randomUUID()
    private lateinit var webSocket: WebSocket
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val receivedMessages = mutableStateOf("")
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    private val locationUpdateInterval = 5000L // 5 saniye
    private val locationHandler = Handler(Looper.getMainLooper())
    private lateinit var locationRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrendyolCanliKonumTakipMobilTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        SimpleButton(
                            onClick = { checkLocationPermission() },
                            messages = receivedMessages.value
                        )
                    }
                }
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initiateWebSocket()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationRunnable = object : Runnable {
                override fun run() {
                    getLastKnownLocation()
                    locationHandler.postDelayed(this, locationUpdateInterval)
                }
            }
            locationHandler.post(locationRunnable)
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendLocationMessage(uuid.toString(), location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Konum alınamadı", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Konum alma hatası: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // İzinler uygun değilse yeniden izin isteği yapılabilir
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun sendLocationMessage(uuid: String, lat: Double, lng: Double) {
        val message = mapOf("uuid" to uuid, "lat" to lat, "lng" to lng)
        val jsonMessage = Gson().toJson(message)
        if (::webSocket.isInitialized) {
            webSocket.send(jsonMessage)
        }
    }

    private fun initiateWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://192.168.1.33:8080/ws/mobile")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Connected!")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived: $text"
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived bytes: ${bytes.utf8()}"
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                runOnUiThread {
                    Log.d("Test123", "WebSocket Closing: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Error: ${t.message}")
                }
                // Yeniden bağlantıyı başlat
                reconnectWebSocket()
            }
        }

        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    private fun reconnectWebSocket() {
        Handler(Looper.getMainLooper()).postDelayed({
            initiateWebSocket()
        }, 3000) // 3 saniye sonra yeniden bağlanmayı dene
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHandler.removeCallbacks(locationRunnable)
        webSocket.close(1000, "App closing")
    }
}

@Composable
fun SimpleButton(onClick: () -> Unit, messages: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Button(
        onClick = {
            onClick()
            Toast.makeText(context, "Message Sent!", Toast.LENGTH_SHORT).show()
        },
        modifier = modifier
    ) {
        Text(text = "Send Location")
    }
    Text(text = messages)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TrendyolCanliKonumTakipMobilTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            SimpleButton(onClick = {}, messages = "")
        }
    }
}*/


/*
class MainActivity : ComponentActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val ctx = applicationContext
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
        Configuration.getInstance().userAgentValue = "MapApp"

        setContent {
            TrendyolCanliKonumTakipMobilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(modifier = Modifier.weight(1f)) {
                            OsmdroidMapView(fusedLocationClient)
                        }
                        Spacer(modifier = Modifier.height(16.dp)) // Optional spacing between map and button
                        Button(
                            onClick = { */
/* Handle button click *//*
 },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp) // Add padding around the button
                        ) {
                            Text(text = "Click Me")
                        }
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, you can proceed
        }
    }
}

@Composable
fun OsmdroidMapView(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setBuiltInZoomControls(true)
            mapView.setMultiTouchControls(true)

            val zoomLevel = 19.0
            val mapController = mapView.controller
            mapController.setZoom(zoomLevel)

            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationTask: Task<Location> = fusedLocationClient.lastLocation
                locationTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val location = task.result
                        if (location != null) {
                            val userLocation = GeoPoint(location.latitude, location.longitude)
                            mapController.setCenter(userLocation)

                            val marker = Marker(mapView)
                            marker.position = userLocation
                            marker.title = "You are here"

                            */
/*val markerIcon = ContextCompat.getDrawable(ctx, R.drawable.red_marker)
                            marker.icon = markerIcon*//*


                            mapView.overlays.add(marker)
                        }
                    }
                }
            }

            mapView
        }
    )
}*/


/*

// Bu kod servise istek atıyor ve çalışıyordu. Birden harita yüklenmemeye başladı

class MainActivity : ComponentActivity() {
    private val uuid = UUID.randomUUID()
    private lateinit var webSocket: WebSocket
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val receivedMessages = mutableStateOf("")
    private val locationPermissionRequest = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    private val locationUpdateInterval = 5000L // 5 saniye
    private val locationHandler = Handler(Looper.getMainLooper())
    private lateinit var locationRunnable: Runnable
    private var isSendingLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TrendyolCanliKonumTakipMobilTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OsmdroidMapView(fusedLocationClient)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (isSendingLocation) {
                                    stopSendingLocation()
                                } else {
                                    checkLocationPermission()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(text = if (isSendingLocation) "Stop Sending Location" else "Start Sending Location")
                        }
                        Text(text = receivedMessages.value)
                    }
                }
            }
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        initiateWebSocket()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                startLocationUpdates()
            }
            else -> {
                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isSendingLocation = true
            locationRunnable = object : Runnable {
                override fun run() {
                    getLastKnownLocation()
                    locationHandler.postDelayed(this, locationUpdateInterval)
                }
            }
            locationHandler.post(locationRunnable)
        } else {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun stopSendingLocation() {
        isSendingLocation = false
        locationHandler.removeCallbacks(locationRunnable)
    }

    private fun getLastKnownLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    sendLocationMessage(uuid.toString(), location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Konum alınamadı", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Konum alma hatası: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            // İzinler uygun değilse yeniden izin isteği yapılabilir
            locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun sendLocationMessage(uuid: String, lat: Double, lng: Double) {
        val message = mapOf("uuid" to uuid, "lat" to lat, "lng" to lng)
        val jsonMessage = Gson().toJson(message)
        if (::webSocket.isInitialized) {
            webSocket.send(jsonMessage)
        }
    }

    private fun initiateWebSocket() {
        val client = OkHttpClient.Builder()
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://192.168.1.33:8080/ws/mobile")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Connected!")
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived: $text"
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                runOnUiThread {
                    receivedMessages.value += "\nReceived bytes: ${bytes.utf8()}"
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                runOnUiThread {
                    Log.d("Test123", "WebSocket Closing: $reason")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                runOnUiThread {
                    Log.d("Test123", "WebSocket Error: ${t.message}")
                }
                // Yeniden bağlantıyı başlat
                reconnectWebSocket()
            }
        }

        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()
    }

    private fun reconnectWebSocket() {
        Handler(Looper.getMainLooper()).postDelayed({
            initiateWebSocket()
        }, 3000) // 3 saniye sonra yeniden bağlanmayı dene
    }

    override fun onDestroy() {
        super.onDestroy()
        locationHandler.removeCallbacks(locationRunnable)
        webSocket.close(1000, "App closing")
    }
}

@Composable
fun OsmdroidMapView(fusedLocationClient: FusedLocationProviderClient) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.setBuiltInZoomControls(true)
            mapView.setMultiTouchControls(true)

            val zoomLevel = 19.0
            val mapController = mapView.controller
            mapController.setZoom(zoomLevel)

            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                val locationTask: Task<Location> = fusedLocationClient.lastLocation
                locationTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val location = task.result
                        if (location != null) {
                            val userLocation = GeoPoint(location.latitude, location.longitude)
                            mapController.setCenter(userLocation)

                            val marker = Marker(mapView)
                            marker.position = userLocation
                            marker.title = "You are here"

                            //val markerIcon = ContextCompat.getDrawable(ctx, R.drawable.red_marker)
                            //marker.icon = markerIcon

                            mapView.overlays.add(marker)
                        }
                    }
                }
            }

            mapView
        }
    )
}
*/

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.views.MapView
import okhttp3.*
import com.google.gson.Gson
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {

    private lateinit var mapView: MapView
    private lateinit var myLocationOverlay: MyLocationNewOverlay
    private lateinit var webSocket: WebSocket
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private var isSendingLocation by mutableStateOf(false)
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private val TAG = "WebSocketExample"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize OSMDroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))

        // Initialize permission launcher
        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Log.e(TAG, "Location permission denied")
            }
        }
        setContent {
            // Use a Box to stack content on top of each other
            Box(modifier = Modifier.fillMaxSize()) {
                // Map view
                MapViewComposable()

                // Button at the bottom left
                Button(
                    onClick = { toggleLocationSending() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text("Send Location")
                }
            }
        }
        // WebSocket setup
        setupWebSocket()

        // Request permission
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    @Composable
    private fun MapViewComposable() {
        mapView = remember { MapView(this) }
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)

        myLocationOverlay = MyLocationNewOverlay(mapView)
        mapView.overlays.add(myLocationOverlay)

        // Display the map view
        AndroidView({ mapView }, modifier = Modifier.fillMaxSize())
    }

    private fun setupWebSocket() {
        val client = OkHttpClient()
        val request = Request.Builder().url("ws://192.168.1.33:8080/ws/mobile").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Message received: $text")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error", t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $reason")
            }
        })
    }

    private fun toggleLocationSending() {
        isSendingLocation = !isSendingLocation
        if (isSendingLocation) {
            sendLocationPeriodically()
        } else {
            Log.d(TAG, "Stopped sending location updates")
        }
    }

    private fun sendLocationPeriodically() {
        thread {
            while (isSendingLocation) {
                val location = myLocationOverlay.myLocation
                if (location != null) {
                    sendLocationMessage(location.latitude, location.longitude)
                } else {
                    Log.d(TAG, "Location is null")
                }
                Thread.sleep(1000)
            }
        }
    }

    private fun sendLocationMessage(lat: Double, lng: Double) {
        val message = mapOf("type" to "location", "lat" to lat, "lng" to lng)
        val jsonMessage = gson.toJson(message)
        Log.d(TAG, "Sending message: $jsonMessage")
        webSocket.send(jsonMessage)
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation()
            myLocationOverlay.enableFollowLocation()
            mapView.invalidate()
            Log.d(TAG, "Location updates started")
        } else {
            // Request permission if not granted
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
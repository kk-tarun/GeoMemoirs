package com.example.geomemoirs.ui.map

import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.geomemoirs.Note
import com.example.geomemoirs.R
import com.example.geomemoirs.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener{

    private var _binding: FragmentMapBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!


    private lateinit var mapView: MapView
    private lateinit var googleMap: GoogleMap
    private lateinit var database: FirebaseDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val root: View = binding.root

        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        database = FirebaseDatabase.getInstance()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isCompassEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true

        showCurrentUserLocation()

        googleMap.setOnMapClickListener { latLng ->
            // Handle map click event here
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))

            val address = getAddressFromLocation(latLng.latitude, latLng.longitude)

            val markerOptions = MarkerOptions().position(latLng).title(address?.getAddressLine(0).toString())
            Log.i("Address", address.toString());
            googleMap.addMarker(markerOptions)

            // Show add note dialog
            showAddNoteDialog(latLng)

            Toast.makeText(requireContext(), "Clicked at: $latLng", Toast.LENGTH_SHORT).show()
        }

        // Set marker click listener
        googleMap.setOnMarkerClickListener(this)

        // Load existing notes from database
        loadNotesFromDatabase()
    }

    private fun showCurrentUserLocation() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val usersRef = database.reference.child("users").child(uid)
            usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val locationData = dataSnapshot.child("current_location").getValue() as HashMap<String, Any>?
                    locationData?.let {
                        val latitude = it["latitude"] as Double
                        val longitude = it["longitude"] as Double
                        val userLocation = LatLng(latitude, longitude)
                        // Move camera to user's location
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f))
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(requireContext(), "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): Address? {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()
        } catch (e: IOException) {
            null
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun showAddNoteDialog(latLng: LatLng) {
        val dialogView = layoutInflater.inflate(R.layout.fragment_add_note_dialog, null)
        val editTextNote = dialogView.findViewById<EditText>(R.id.editTextNote)
        val buttonAdd = dialogView.findViewById<Button>(R.id.buttonAdd)

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Add Note")

        val alertDialog = dialogBuilder.create()

        buttonAdd.setOnClickListener {
            val noteText = editTextNote.text.toString().trim()
            if (noteText.isNotEmpty()) {
                // Call onNoteAdded method to handle adding the note
                onNoteAdded(noteText, latLng)

                alertDialog.dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter note text", Toast.LENGTH_SHORT).show()
            }
        }

        alertDialog.show()
    }

    private fun onNoteAdded(note: String, latLng: LatLng) {
        Log.i("location", latLng.toString())
        // Save the note and location to the database
        saveNoteToDatabase(note, latLng.latitude, latLng.longitude)
    }

    private fun saveNoteToDatabase(note: String, latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val noteObj = Note(note, latitude, longitude)
            val notesRef = database.reference.child("users").child(uid).child("notes")
            val newNoteRef = notesRef.push()
            newNoteRef.setValue(noteObj)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Note added successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Failed to add note: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        val noteText = marker.title

        // Show note in dialog box
        if (noteText != null && noteText != "Current Location") {
            showNoteDialog(noteText)
        }

        return true // Return true to indicate that the click event has been handled
    }

    private fun showNoteDialog(noteText: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note, null)
        val textViewNote = dialogView.findViewById<TextView>(R.id.textViewNote)

        textViewNote.text = noteText

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Note")

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun loadNotesFromDatabase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val notesRef = database.reference.child("users").child(uid).child("notes")
            notesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (noteSnapshot in dataSnapshot.children) {
                        val note = noteSnapshot.getValue(Note::class.java)
                        note?.let {
                            val noteLocation = LatLng(it.latitude, it.longitude)
                            val markerOptions = MarkerOptions().position(noteLocation).title(it.text)
                            googleMap.addMarker(markerOptions)
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(requireContext(), "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

}
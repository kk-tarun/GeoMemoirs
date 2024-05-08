package com.example.geomemoirs.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.geomemoirs.Note
import com.example.geomemoirs.R
import com.example.geomemoirs.databinding.FragmentHomeBinding
import com.example.geomemoirs.ui.notes.NoteAdapter
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.IOException
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var recyclerView: RecyclerView
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var noteList: MutableList<Note>

    private lateinit var database: FirebaseDatabase

    private val LOCATION_PERMISSION_REQUEST_CODE = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewNotes)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        noteList = mutableListOf()
        noteAdapter = NoteAdapter(noteList)
        recyclerView.adapter = noteAdapter

        database = FirebaseDatabase.getInstance()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Request location permission if not granted
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, display current location
            displayCurrentLocation()
        }

        loadNotesFromDatabase()

        return view
    }

    private fun loadNotesFromDatabase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let { uid ->
            val notesRef = database.reference.child("users").child(uid).child("notes")
            notesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    noteList.clear()
                    for (noteSnapshot in dataSnapshot.children) {
                        val note = noteSnapshot.getValue(Note::class.java)
                        note?.let {
                            noteList.add(it)
                        }
                    }
                    noteAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle database error
                    Toast.makeText(requireContext(), "Database error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun displayCurrentLocation() {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 10000 // 10 seconds

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    val address = getAddressFromLocation(location.latitude, location.longitude)
                    val locationText = address?.let { getAddressText(it) } ?: "Location not found"

                    // Save current location to Firebase Database
                    saveCurrentLocationToFirebase(location.latitude, location.longitude)

                    _binding?.placeholderTextView?.text = locationText
                    fusedLocationClient.removeLocationUpdates(this)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
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

    private fun getAddressText(address: Address): String {
        val city = address.locality
        val state = address.adminArea
        val country = address.countryName
        return "$city, $state, $country"
    }

    private fun saveCurrentLocationToFirebase(latitude: Double, longitude: Double) {
        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid

        userId?.let { uid ->
            val locationData = HashMap<String, Any>()
            locationData["latitude"] = latitude
            locationData["longitude"] = longitude

            val userData = HashMap<String, Any>()
            userData["current_location"] = locationData

            val usersRef = FirebaseDatabase.getInstance().reference.child("users")
            usersRef.child(uid).updateChildren(userData)
                .addOnSuccessListener {
                    // Successfully saved current location to Firebase
                    Toast.makeText(context, "Successfully updated the current location", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(context, "Unable to update current location", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

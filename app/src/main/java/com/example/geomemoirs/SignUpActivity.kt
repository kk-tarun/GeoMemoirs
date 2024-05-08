package com.example.geomemoirs

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.location.LocationManagerCompat.getCurrentLocation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val btnSignUp: Button = findViewById(R.id.btnSignUp);
        val etEmail: EditText = findViewById(R.id.etEmail);
        val etPassword: EditText = findViewById(R.id.etPassword);
        val etConfirmPassword: EditText = findViewById(R.id.etConfirmPassword);
        val tvLogin: TextView = findViewById(R.id.tvLogin);
        val etName: EditText = findViewById(R.id.etName);

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        usersRef = database.reference.child("users")

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()
            val name = etName.text.toString();


            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && password == confirmPassword) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign up successful, add user data to database
                            val user = auth.currentUser
                            val userId = user?.uid

                            // Add user data to database
                            userId?.let { uid ->
                                val userData = HashMap<String, Any>()
                                userData["email"] = email
                                userData["name"] = name

                                usersRef.child(uid).setValue(userData)
                                    .addOnSuccessListener {
                                        Toast.makeText(baseContext, "Registration successful", Toast.LENGTH_SHORT).show()
                                        val intent = Intent(this, LoginActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { exception ->
                                        Toast.makeText(baseContext, "Registration failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        } else {
                            // If registration fails, display a message to the user.
                            Toast.makeText(baseContext, "Registration failed. ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else if(name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
                Toast.makeText(baseContext, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            } else if(password != confirmPassword){
                Toast.makeText(baseContext, "Passwords do not match", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(baseContext, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

package com.example.realtimetrackingsystem;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class login extends AppCompatActivity {
    private EditText emailEditText1, passEditText1;
    private MaterialButton login1;
    private ProgressBar progressBar;
    private static final String TAG = "login";
    private boolean passwordVisible = false;
    String email1;
    String driverNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView forgot = findViewById(R.id.forgot_pass);
        TextView signUpTextView = findViewById(R.id.sign_up_text_view);
        TextView track =findViewById(R.id.track);

        login1 = findViewById(R.id.login_btn);
        emailEditText1 = findViewById(R.id.log_email);
        passEditText1 = findViewById(R.id.log_pass);
        progressBar = findViewById(R.id.progressBar);
        TextPaint paint = track.getPaint();
        float width = paint.measureText(track.getText().toString());


// Create a LinearGradient for text color
        Shader textShader = new LinearGradient(
                0, 0, width, track.getTextSize(),
                new int[]{
                        Color.parseColor("#BC7B33"),
                        Color.parseColor("#E0903C"),
                        Color.parseColor("#323131"),
                        Color.parseColor("#0A0909")
                },
                null,
                Shader.TileMode.CLAMP);

        track.getPaint().setShader(textShader);
        if (SharedPreferencesHelper.isLoggedIn(this)) {
            driverNumber = SharedPreferencesHelper.getDriverNumber(this);
            launchAppropriateActivity(driverNumber);
            finish();
        }
        login1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email1 = emailEditText1.getText().toString();
                String pass1 = passEditText1.getText().toString();
                if (TextUtils.isEmpty(email1) || TextUtils.isEmpty(pass1)) {
                    Toast.makeText(login.this, "Enter all the fields properly", Toast.LENGTH_SHORT).show();
                } else {
                    showProgressBar();
                    log(email1, pass1);
                }
            }
        });

        forgot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(login.this, forgotpass.class);
                startActivity(intent);
            }
        });

        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(login.this, SignUpActivity.class);
                startActivity(intent);
            }
        });

        passEditText1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getRawX() >= (passEditText1.getRight() - passEditText1.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width())) {
                        togglePasswordVisibility();
                        return true;
                    }
                }
                return false;
            }
        });

        hidePassword();

    }

    private void togglePasswordVisibility() {
        if (passwordVisible) {
            hidePassword();
        } else {
            showPassword();
        }
    }

    private void showPassword() {
        passEditText1.setTransformationMethod(null);
        passwordVisible = true;
    }

    private void hidePassword() {
        passEditText1.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordVisible = false;
    }

    private void log(final String email1, final String pass1) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference userRef = db.collection("users").document(email1);

        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String storedPassword = document.getString("password");
                        if (pass1.equals(storedPassword)) {
                            String userType = document.getString("usertype");
                            String userName = document.getString("Name");
                            SharedPreferencesHelper.setUserName(login.this,userName);
                            String usermail = document.getString("email");
                            Log.d(TAG, "useremail: " + usermail);
                            SharedPreferencesHelper.setEmailId(login.this,usermail);
                            driverNumber = document.getString("phone");
                            SharedPreferencesHelper.setDriverNumber(login.this, driverNumber);
                            Intent locationServiceIntent = new Intent(login.this, LocationSharingService.class);
                            locationServiceIntent.setAction(LocationSharingService.ACTION_START);
                            locationServiceIntent.putExtra("driverNumber", driverNumber); // Pass the driverNumber
                            startService(locationServiceIntent);
                            SharedPreferencesHelper.setLoggedIn(login.this, true);
//                            SharedPreferencesHelper.setUserType(login.this, userType);
                            launchAppropriateActivity(driverNumber);
                        } else {
                            handleLoginFailure();
                        }
                    } else {
                        handleUserNotFound();
                    }
                } else {
                    handleLoginError();
                }
            }
        });
    }
private void launchAppropriateActivity(String userPhoneNumber) {
    // Assuming you have a reference to your Firestore database
    FirebaseFirestore db = FirebaseFirestore.getInstance();

    CollectionReference phoneNumberCollection = db.collection("phonenumbers");

    phoneNumberCollection.document(userPhoneNumber).get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Driver's phone number found, launch driver activity
                        SharedPreferencesHelper.setUserType(login.this,"driver");
                        Intent intent = new Intent(login.this, driverlocationparmission.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Regular user, launch home activity
                        SharedPreferencesHelper.setUserType(login.this,"user");
                        Intent intent = new Intent(login.this, driverlocationparmission.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    // Error handling
                    Toast.makeText(login.this, "Error fetching user data.", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                }
            });
}

    private void handleLoginFailure() {
        Toast.makeText(login.this, "Login Failed: Incorrect credentials", Toast.LENGTH_SHORT).show();
        hideProgressBar();
    }

    private void handleUserNotFound() {
        Toast.makeText(login.this, "User data not found.", Toast.LENGTH_SHORT).show();
        hideProgressBar();
    }

    private void handleLoginError() {
        Toast.makeText(login.this, "Error getting user data.", Toast.LENGTH_SHORT).show();
        hideProgressBar();
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
        login1.setEnabled(false);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
        login1.setEnabled(true);
    }
}

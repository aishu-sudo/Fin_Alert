package edu.ewubd.finalert;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class Auth extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        Button btnLogin  = findViewById(R.id.btnLogin);
        Button btnSignup = findViewById(R.id.btnSignup);

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(Auth.this, edu.ewubd.finalert.On_boardingA.class);
            intent.putExtra("destination", "login");
            startActivity(intent);
        });

        btnSignup.setOnClickListener(v -> {
            Intent intent = new Intent(Auth.this, edu.ewubd.finalert.On_boardingA.class);
            intent.putExtra("destination", "signup");
            startActivity(intent);
        });
    }
}

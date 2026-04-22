package edu.ewubd.finalert;

import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class On_boardingB extends AppCompatActivity {

    private GestureDetector gestureDetector;
    private String destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_on_boarding_b);

        destination = getIntent().getStringExtra("destination");

        TextView btnNext = findViewById(R.id.btnNext);
        btnNext.setOnClickListener(v -> goToNext());

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float vX, float vY) {
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > 100 && Math.abs(vX) > 100) {
                        goToNext();
                        return true;
                    }
                return false;
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void goToNext() {
        Intent intent;
        if ("signup".equals(destination)) {
            intent = new Intent(On_boardingB.this, SignUp.class);
        } else {
            intent = new Intent(On_boardingB.this, Log_in.class);
        }
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }
}

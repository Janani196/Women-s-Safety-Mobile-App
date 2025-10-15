package com.example.my;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;

public class SelfDefenseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_defense);

        // Initialize and set up the TextView to show tips
        TextView tipsTextView = findViewById(R.id.tips_text_view);
        String tips = getString(R.string.self_defense_tips);
        tipsTextView.setText(tips);
    }
}

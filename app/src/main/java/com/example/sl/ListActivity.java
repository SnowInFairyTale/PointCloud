package com.example.sl;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ListActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.normal).setOnClickListener(v -> {
            startPage("normal");
        });
        findViewById(R.id.circle).setOnClickListener(v -> {
            startPage("circle");
        });
        findViewById(R.id.fast).setOnClickListener(v -> {
            startPage("fast");
        });
        findViewById(R.id.light).setOnClickListener(v -> {
            startPage("light");
        });
        findViewById(R.id.smooth).setOnClickListener(v -> {
            startPage("smooth");
        });
        findViewById(R.id.quality).setOnClickListener(v -> {
            startPage("quality");
        });
        findViewById(R.id.mesh).setOnClickListener(v -> {
            Intent intent = new Intent(ListActivity.this, MeshActivity.class);
            startActivity(intent);
        });

    }

    private void startPage(String mode) {
        Intent intent = new Intent(ListActivity.this, MainActivity.class);
        intent.putExtra("mode", mode);
        startActivity(intent);
    }
}

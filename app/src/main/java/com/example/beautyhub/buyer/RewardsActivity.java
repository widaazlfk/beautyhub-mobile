package com.example.beautyhub.buyer;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.beautyhub.R;

public class RewardsActivity extends AppCompatActivity {

    private RecyclerView rvRewards;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        // Setup rewards grid
        rvRewards = findViewById(R.id.rv_rewards);
        rvRewards.setLayoutManager(new GridLayoutManager(this, 2));

        // Setup adapter for rewards
        // RewardsAdapter adapter = new RewardsAdapter(rewardsList);
        // rvRewards.setAdapter(adapter);
    }
}
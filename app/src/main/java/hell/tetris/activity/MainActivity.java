package hell.tetris.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import hell.tetris.R;
import hell.tetris.databinding.ActiviyMainBinding;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActiviyMainBinding binding = DataBindingUtil.setContentView(this, R.layout.activiy_main);
    }

    public void onMenuSelect(View v) {
        switch (v.getId()) {
            case R.id.btn_game_start:
                startActivity(new Intent(MainActivity.this, GameBoardActivity.class));
                break;

            case R.id.btn_how_to_play:
                startActivity(new Intent(MainActivity.this, HowToPlayActivity.class));
                break;

            case R.id.btn_exit:
                finish();
                break;
        }
    }
}
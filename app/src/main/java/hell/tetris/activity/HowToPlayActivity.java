package hell.tetris.activity;

import android.app.*;
import android.os.*;
import android.view.*;

import androidx.databinding.DataBindingUtil;
import hell.tetris.R;
import hell.tetris.databinding.ActivityHowToPlayBinding;

public class HowToPlayActivity extends Activity {

    ActivityHowToPlayBinding binding;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_how_to_play);
    }

    public void onClickBack(View v) {
        finish();
    }
}
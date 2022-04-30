package hell.tetris.activity;

import android.app.*;
import android.os.*;
import android.view.*;

import hell.tetris.R;

public class HowToPlayActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.how_to_play);
	}
    
    public void backToHome(View v)
    {
    	finish();
    }
}
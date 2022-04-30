package hell.tetris.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import hell.tetris.R;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    public void onMenuSelect(View v)
    {
    	switch(v.getId())
    	{
    	case R.id.game:
    		startActivity(new Intent(MainActivity.this, GameBoardActivity.class));
    	break;
    	
    	case R.id.how_to_play:
    		startActivity(new Intent(MainActivity.this, HowToPlayActivity.class));
    	break;
    	
    	case R.id.exit:
    		finish();
    	break;    		
    	}
    }
}
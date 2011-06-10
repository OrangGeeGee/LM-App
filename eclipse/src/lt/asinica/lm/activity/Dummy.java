package lt.asinica.lm.activity;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class Dummy extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		TextView t = new TextView(this);
		t.setText(".");
		setContentView(t);
	}
}

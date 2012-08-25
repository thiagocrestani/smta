package mei.tcd.smta.servicefusion;

import mei.tcd.smta.R;
import mei.tcd.smta.util.WGS84;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

public class ServiceActivity extends Activity {
	private TextView debugView;
	private InsGpsReceiver receiver;
	private WGS84 coordenadas;
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.debug);
		debugView = (TextView) findViewById(R.id.textView1);
		// Register Service
        Intent serviceIntent = new Intent(this, ServiceFusao.class);
        startService(serviceIntent);
        // Register Broadcast
        IntentFilter filter = new IntentFilter(InsGpsReceiver.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new InsGpsReceiver();
        registerReceiver(receiver, filter);
       
	}
	public class InsGpsReceiver extends BroadcastReceiver {
		   public static final String ACTION_RESP =    
		      "mei.tcd.smta.servicefusion.MESSAGE_PROCESSED";
		    
		   @Override
		   public void onReceive(Context context, Intent intent) {
		      // TextView result = (TextView) findViewById(R.id.txt_result);
		      //String text = intent.getStringExtra(SimpleIntentService.PARAM_OUT_MSG);
		      // result.setText(text);
			   debugView.setText(Double.toString(intent.getExtras().getDouble(ServiceFusao.LATITUDE)));
			   debugView.append(Double.toString(intent.getExtras().getDouble(ServiceFusao.LONGITUDE)));
		   }
		}
}

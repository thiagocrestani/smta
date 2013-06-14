package mei.tcd.smta.sensorinfo;

import java.util.ArrayList;
import java.util.List;

import mei.tcd.smta.BuildConfig;
import mei.tcd.smta.R;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SensorListActivity extends ListActivity {
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.listasensoreslayout);
        SensorManager sManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);// Para aceder aos sensores tenho de inicializar a instancia
        List<Sensor> sensores = sManager.getSensorList(Sensor.TYPE_ALL); // Lista que mantem os elementos ordenados. Carrego a lista com os sensores encontrados.
        List<Sensor> sensoresReais = new ArrayList<Sensor>();
        int i=0;
        for (Sensor sensor : sensores) {
            Log.d("Sensors", "" + sensor.getName());
            //sensoresReais.add(i,sensor);
            if(!sensor.getName().contains("placeholder"))
            {
            	sensoresReais.add(i,sensor);
            	 i++;
            }
           
        }
        this.setListAdapter(new SensorListAdapter(this,android.R.layout.simple_list_item_1,sensoresReais));
    }
	// Adapter para listView para mostrar os nomes dos sensores e responder a cliques
	private class SensorListAdapter extends ArrayAdapter<Sensor>
	{

		public SensorListAdapter(Context context, 	int textViewResourceId, List<Sensor> objects) {
			super(context,  textViewResourceId, objects);
			
		}
		// Cria uma view com o nome do sensor
		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			final Sensor selectedSensor = getItem(position);
			if(convertView==null)
			{
				// Layout inflater - Instancia o ficheiro XML do layout para o corresnponde objecto view
				convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1,null);
			}
			((TextView) convertView).setText(selectedSensor.getName());
			convertView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					if(BuildConfig.DEBUG)
					{
						Log.d("teste","display sensor" + selectedSensor.getName());
					}
					//Carrega a actividade como argumento do sensor
					//showActivity();
					
					Intent i = new Intent(SensorListActivity.this,mei.tcd.smta.sensorinfo.SensorInfo.class);
					i.putExtra("sensor", selectedSensor.getName());
		    		//O startactivity também é do context
		    		startActivity(i);
				}
			});
			return convertView;
		}
		
		
	}

}

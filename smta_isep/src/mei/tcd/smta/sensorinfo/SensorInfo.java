package mei.tcd.smta.sensorinfo;

import java.util.List;

import mei.tcd.smta.BuildConfig;
import mei.tcd.smta.R;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.view.LayoutInflater;
//classe que implementa os métodos do sensoreventlistener, logo override
public class SensorInfo extends Activity implements SensorEventListener{
	private static final String THETA = "\u0398";
	private static final String ACCELERATION_UNITS = "m/s\u00b2";
	private SensorManager sManager;
    private Sensor sensor;
    private TextView name;
    private TextView type;
    private TextView maxRange;
    private TextView minDelay;
    private TextView power;
    private TextView resolution;
    private TextView vendor;
    private TextView version;
    private TextView accuracy;
    private TextView timestampLabel;
    private TextView timestamp;
    private TextView timestampUnits;
    private TextView dataLabel;
    private TextView dataUnits;
    private TextView xAxis;
    private TextView xAxisLabel;
    private TextView yAxis;
    private TextView yAxisLabel;
    private TextView zAxis;
    private TextView zAxisLabel;
    private TextView singleValue;
    private TextView cosLabel;
    private TextView cos;
    
    String sensorString = "";
    
    @Override
    public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.sensor_info);    
    	// Ok tentativa de buscar os parametros passados
    	// Vou buscar o intent que iniciou esta actividade
    	Bundle extras = getIntent().getExtras(); 
    	if(extras !=null)
    	{
    		sensorString = extras.getString("sensor");
    	}
    	
    	
    	
    	LayoutInflater inflater = (LayoutInflater)this.getSystemService        (Context.LAYOUT_INFLATER_SERVICE);
    	View layout = inflater.inflate(R.layout.sensor_info, null);
    	sManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);// Para aceder aos sensores tenho de inicializar a instancia
    	List<Sensor> msensorList = sManager.getSensorList(Sensor.TYPE_ALL);// Listo sensores
    	String sensorTmp = "";
    	for (int i=0;i<msensorList.size();i++){// Ciclo para verificar que tipo de sensores pelo nome recebido
    		sensorTmp = msensorList.get(i).getName().toString().trim();
    		// não usar == pois compara objectos
    		if(sensorTmp.equals(sensorString.trim())){
    			sensor = sManager.getDefaultSensor(msensorList.get(i).getType());
    		}
    	}
    	
    	
    	name= (TextView) findViewById(R.id.nome);
    	type = (TextView) findViewById(R.id.type);
        maxRange = (TextView) findViewById(R.id.maxRange);
        minDelay = (TextView) findViewById(R.id.minDelay);
        power = (TextView) findViewById(R.id.power);
        resolution = (TextView) findViewById(R.id.resolution);
        vendor = (TextView) findViewById(R.id.vendor);
        version = (TextView) findViewById(R.id.version);
        accuracy = (TextView) findViewById(R.id.accuracy);
        timestampLabel = (TextView) findViewById(R.id.timestampLabel);
        timestamp = (TextView) findViewById(R.id.timestamp);
        timestampUnits = (TextView) findViewById(R.id.timestampUnits);
        dataLabel = (TextView) findViewById(R.id.dataLabel);
        dataUnits = (TextView) findViewById(R.id.dataUnits);
        xAxis = (TextView) findViewById(R.id.xAxis);
        xAxisLabel = (TextView) findViewById(R.id.xAxisLabel);
        yAxis = (TextView) findViewById(R.id.yAxis);
        yAxisLabel = (TextView) findViewById(R.id.yAxisLabel);
        zAxis = (TextView) findViewById(R.id.zAxis);
        zAxisLabel = (TextView) findViewById(R.id.zAxisLabel);
        singleValue = (TextView) findViewById(R.id.singleValue);
        cosLabel = (TextView) findViewById(R.id.cosLabel);
        cos = (TextView) findViewById(R.id.cos);
        // Click listener para fastest radio button
        
        name.setText(sensor.getName());
        type.setText(String.valueOf(sensor.getType()));
        maxRange.setText(String.valueOf(sensor.getMaximumRange()));
        minDelay.setText(String.valueOf(sensor.getMinDelay()));
        power.setText(String.valueOf(sensor.getPower()));
        resolution.setText(String.valueOf(sensor.getResolution()));
        vendor.setText(String.valueOf(sensor.getVendor()));
        version.setText(String.valueOf(sensor.getVersion()));
        // Por defeito faz o register listener para normal
        sManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        findViewById(R.id.S_DELAY_FASTEST).setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View v)
        	{
        		sManager.unregisterListener(SensorInfo.this);
        		sManager.registerListener(SensorInfo.this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        		
        	}
        });
        // Click listener para delaygame radio button
        findViewById(R.id.S_DELAY_GAME).setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View v)
        	{
        		sManager.unregisterListener(SensorInfo.this);
        		sManager.registerListener(SensorInfo.this, sensor, SensorManager.SENSOR_DELAY_GAME);
        		
        	}
        });
        // Click listener para normal radio button
        findViewById(R.id.S_DELAY_NORMAL).setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View v)
        	{
        		sManager.unregisterListener(SensorInfo.this);
        		sManager.registerListener(SensorInfo.this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        		
        	}
        });
     // Click listener para UI radio button
        findViewById(R.id.S_DELAY_UI).setOnClickListener(new OnClickListener()
        {
        	@Override
        	public void onClick(View v)
        	{
        		sManager.unregisterListener(SensorInfo.this);
        		sManager.registerListener(SensorInfo.this, sensor, SensorManager.SENSOR_DELAY_UI);
        		
        	}
        });
        
    }
	@Override
	public void onSensorChanged(SensorEvent event) {
		onAccuracyChanged(event.sensor, event.accuracy);
        timestampLabel.setVisibility(View.VISIBLE);
        timestamp.setVisibility(View.VISIBLE);
        timestamp.setText(String.valueOf(event.timestamp));
        timestampUnits.setVisibility(View.VISIBLE);
        switch (event.sensor.getType())
        {
       
            case Sensor.TYPE_ACCELEROMETER:
                showEventData("Acceleration - gravity on axis",
                        ACCELERATION_UNITS,
                        event.values[0],
                        event.values[1],
                        event.values[2]+0.44f); //*1.044f
                break;
                
            case Sensor.TYPE_MAGNETIC_FIELD:
                showEventData("Abient Magnetic Field",
                        "uT",
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                showEventData("Angular speed around axis",
                        "radians/sec",
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                break;
            case Sensor.TYPE_LIGHT:
                showEventData("Ambient light",
                        "lux",
                        event.values[0]);
                break;
            case Sensor.TYPE_PRESSURE:
                showEventData("Atmospheric pressure",
                        "hPa",
                        event.values[0]);
                break;
            case Sensor.TYPE_PROXIMITY:
                showEventData("Distance",
                        "cm",
                        event.values[0]);
                break;
            case Sensor.TYPE_GRAVITY:
                showEventData("Gravity",
                        ACCELERATION_UNITS,
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                showEventData("Acceleration (not including gravity)",
                        ACCELERATION_UNITS,
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                
                showEventData("Rotation Vector",
                        null,
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                
                xAxisLabel.setText("x*sin(" + THETA + "/2)");
                yAxisLabel.setText("y*sin(" + THETA + "/2)");
                zAxisLabel.setText("z*sin(" + THETA + "/2)");
                
                if (event.values.length == 4)
                {
                    cosLabel.setVisibility(View.VISIBLE);
                    cos.setVisibility(View.VISIBLE);
                    cos.setText(String.valueOf(event.values[3]));
                }
                
                break;
            case Sensor.TYPE_ORIENTATION:
                showEventData("Angle",
                        "Degrees",
                        event.values[0],
                        event.values[1],
                        event.values[2]);
                
                xAxisLabel.setText("Azimuth");
                yAxisLabel.setText("Pitch");
                zAxisLabel.setText("Roll");
                
                break;
            
        }
		
	}
	private void showEventData(String label, String units, float x, float y, float z)
    {
        dataLabel.setVisibility(View.VISIBLE);
        dataLabel.setText(label);
        
        if (units == null)
        {
            dataUnits.setVisibility(View.GONE);
        }
        else
        {
            dataUnits.setVisibility(View.VISIBLE);
            dataUnits.setText("(" + units + "):");
        }
        
        singleValue.setVisibility(View.GONE);
        
        xAxisLabel.setVisibility(View.VISIBLE);
        xAxisLabel.setText("X");
        xAxis.setVisibility(View.VISIBLE);
        xAxis.setText(String.valueOf(x));
        
        yAxisLabel.setVisibility(View.VISIBLE);
        yAxisLabel.setText("Y");
        yAxis.setVisibility(View.VISIBLE);
        yAxis.setText(String.valueOf(y));
        
        zAxisLabel.setVisibility(View.VISIBLE);
        zAxisLabel.setText("Z");
        zAxis.setVisibility(View.VISIBLE);
        zAxis.setText(String.valueOf(z));
    }
    
    private void showEventData(String label, String units, float value)
    {
        dataLabel.setVisibility(View.VISIBLE);
        dataLabel.setText(label);
        
        dataUnits.setVisibility(View.VISIBLE);
        dataUnits.setText("(" + units + "):");
        
        singleValue.setVisibility(View.VISIBLE);
        singleValue.setText(String.valueOf(value));
        
        xAxisLabel.setVisibility(View.GONE);
        xAxis.setVisibility(View.GONE);
        
        yAxisLabel.setVisibility(View.GONE);
        yAxis.setVisibility(View.GONE);
        
        zAxisLabel.setVisibility(View.GONE);
        zAxis.setVisibility(View.GONE);
    }
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		switch(accuracy)
		{
		case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
			this.accuracy.setText("SENSOR_STATUS_ACCURACY_HIGH");
			break;
		case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
			this.accuracy.setText("SENSOR_STATUS_ACCURACY_LOW");
			break;
		case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
			this.accuracy.setText("SENSOR_STATUS_ACCURACY_MEDIUM");
			break;
		
		}
		
	}
	 @Override
	    public void onPause()
	    {
	        super.onPause();
	        
	        if (BuildConfig.DEBUG)
	        {
	            Log.d("Log", "onPause");
	            Log.d("Log", "Unregistering listener");
	        }
	        
	        sManager.unregisterListener(this);
	    }

}

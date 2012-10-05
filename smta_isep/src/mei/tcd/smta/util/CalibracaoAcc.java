package mei.tcd.smta.util;

import mei.tcd.smta.ins.Calibracao;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CalibracaoAcc implements SensorEventListener {
	//private static final int OBSERVACOES = 100;
	private float[] dadosAcc = new float[3];
	private Calibracao calib;
	private SensorManager sensorManager;
	SharedPreferences prefs;
	private int contadorProgresso = 0;
	private boolean startCalib = false;
	
	public void start(Context context, int observacoes){
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);// Para
		calib = new Calibracao(observacoes);
		prefs = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if(startCalib)
		{
			int tipoEvento = event.sensor.getType();// guarda o tipo de sensor que gerou o evento
			switch (tipoEvento) {
			case Sensor.TYPE_ACCELEROMETER:
				dadosAcc[0] = event.values[0];
				dadosAcc[1] = event.values[1];
				dadosAcc[2] = event.values[2];
				if(!calib.jaCalibrou){
					calib.adicionadaXYZ(dadosAcc);
				}
				else
				{
					// TODO: Verificar se este processo acaba
					sensorManager.unregisterListener(this);
					gravaPreferencias(calib.getCalibVetor());
					
					
				}
				
				contadorProgresso ++;
			}
		}


	}
	public void gravaPreferencias(float[] dadosCalibrados)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("AccCalibX", dadosCalibrados[0]);
		editor.putFloat("AccCalibY", dadosCalibrados[1]);
		editor.putFloat("AccCalibZ", dadosCalibrados[2]);

		editor.commit();
	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}

package mei.tcd.smta.ins;

import mei.tcd.smta.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CalibracaoActivity extends Activity  implements SensorEventListener{
	private static final int OBSERVACOES = 500;
	private float[] dadosAcc = new float[3];
	private Calibracao calib;
	private static final int LIXO=50;//apenas até assentar
	
	TextView calibText;
	ProgressBar progressBar; 
	private SensorManager sensorManager;
	SharedPreferences prefs;
	private int contadorProgresso = 0;
	private boolean startCalib = false;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.calibracao);
		
		calibText = (TextView) this.findViewById(R.id.textView1);
		progressBar = (ProgressBar) this.findViewById(R.id.progressBar1);

		// Referenciar o sensorManager
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);// Para
		calib = new Calibracao(OBSERVACOES);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		calibText.setText("A calibrar...");
		progressBar.setVisibility(View.INVISIBLE);
		calibText.setVisibility(View.INVISIBLE);
		progressBar.setMax(OBSERVACOES+LIXO);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if(startCalib)
		{
			
			int tipoEvento = event.sensor.getType();// guarda o tipo de sensor que gerou o evento
			switch (tipoEvento) {
			case Sensor.TYPE_ACCELEROMETER:
				if(contadorProgresso>=LIXO){
					dadosAcc[0] = event.values[0];
					dadosAcc[1] = event.values[1];
					dadosAcc[2] = event.values[2];
					if(!calib.jaCalibrou){
						calib.adicionadaXYZ(dadosAcc);
					}
					else
					{
						sensorManager.unregisterListener(this);
						gravaPreferencias(calib.getCalibVetor());
						progressBar.setVisibility(View.INVISIBLE);
						calibText.setText("Calibração efectuada!\nEixo X: "+ calib.getCalibVetor()[0] + "\nEixoY: "+calib.getCalibVetor()[1] + "\nEixoZ: "+calib.getCalibVetor()[2]);
					}
				}
				progressBar.setProgress(contadorProgresso);
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
	public void mostraDialogo()
	{
		// Criar uma caixa de alerta
		AlertDialog.Builder alertbox = new AlertDialog.Builder(this);

		//mensagem
		alertbox.setMessage("Para efectuar a calibração, o telemovel deverá estar " +
				"pousado numa superficie plana com o ecrã virado para cima!\n" +
				"Para usar os valores de calibração, deverá ir a configurações e activar o visto em calibração.\n\nPressione OK para continuar...");

		// não necesito de clicklisneter para isto?|?|?
		alertbox.setNeutralButton("Ok", new DialogInterface.OnClickListener() {


			public void onClick(DialogInterface arg0, int arg1) {
				startCalib = true;
				progressBar.setVisibility(View.VISIBLE);
				calibText.setVisibility(View.VISIBLE);
			}
		});


		alertbox.show();

	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onResume() {
		super.onResume();
		// Tornar a calibrar
		calib.jaCalibrou = false;
		startCalib = false;
		calib.resetCalib();
		contadorProgresso =0;
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);
		mostraDialogo();
	}
	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(this);

	}

	@Override
	protected void onPause() {
		super.onPause();
		// Tenho sempre de efetuar o unRegister para prevenir o gasto da bateria
		// device's battery.
		sensorManager.unregisterListener(this);

	}


}

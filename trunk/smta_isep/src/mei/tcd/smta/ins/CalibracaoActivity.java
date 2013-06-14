package mei.tcd.smta.ins;



import mei.tcd.smta.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CalibracaoActivity extends Activity  implements SensorEventListener{

	private static final int OBSERVACOES = 500;
	private float[] dadosAcc = new float[3];
	private Calibracao calib;
	// private String eixo = "";

	private String tipoACalibrar = "";
	private static final int LIXO=50;//apenas até assentar
	private int vetorCalibracao[]  = new int[3];
	// mensagens para calibração dos eixos

	private String mensagemX_high = "Calibração eixo X (9.8), o telemovel deverá estar " +
			"pousado numa superficie plana virado para o lado esquerdo longitudinalmente!\n"; 
	private String mensagemX_low = "Calibração eixo X (-9.8), o telemovel deverá estar " +
			"pousado numa superficie plana virado para o lado direito longitudinalmente!\n"; 

	private String mensagemY_high = "Calibração eixo Y (9.8), o telemovel deverá estar " +
			"pousado numa superficie plana de pé!\n"; 
	private String mensagemY_low = "Calibração eixo Y (-9.8), o telemovel deverá estar " +
			"pousado numa superficie plana de pé virado ao contrário!\n"; 

	private String mensagemZ_high = "Calibração eixo Z (9.8), o telemovel deverá estar " +
			"pousado numa superficie plana deitado com o ecrã virado para cima!\n"; 
	private String mensagemZ_low = "Calibração eixo Z (-9.8), o telemovel deverá estar " +
			"pousado numa superficie plana deitado com o ecrã virado para baixo!\n";
	TextView calibText;
	TextView sensorText;
	TextView mensagem;
	ProgressBar progressBar;
	Button calibrar, continuar;
	Button sair;
	private SensorManager sensorManager;
	SharedPreferences prefs;
	private int contadorProgresso = 0;
	private boolean startCalib = false;
	private float xHigh=0;
	private float xLow=0;
	private float yHigh=0;
	private float yLow=0;
	private float zHigh=0;
	private float zLow=0;
	private float mHigh=0;
	private float mLow=0;
	private float Kx=0;
	private float Ky=0;
	private float Kz=0;
	private float Bx=0;
	private float By=0;
	private float Bz=0;
	// Criar uma caixa de alerta
	AlertDialog.Builder alertbox ;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.calibracao);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		alertbox= new AlertDialog.Builder(this);

		calibText = (TextView) this.findViewById(R.id.textView1);
		sensorText = (TextView) this.findViewById(R.id.textView2);
		mensagem = (TextView) this.findViewById(R.id.mensagem);
		calibrar = (Button) this.findViewById(R.id.calibrarbtn);
		sair = (Button) this.findViewById(R.id.calibrarsair);
		continuar = (Button) this.findViewById(R.id.continuarbtn);
		progressBar = (ProgressBar) this.findViewById(R.id.progressBar1);
		
		// Referenciar o sensorManager
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);// Para
		calib = new Calibracao(OBSERVACOES);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		calibText.setText("A calibrar...");
		sensorText.setText("");
		progressBar.setVisibility(View.INVISIBLE);
//		calibText.setVisibility(View.INVISIBLE);
		progressBar.setMax(OBSERVACOES+LIXO);
		
//		calibrar.setVisibility(View.INVISIBLE);
//		sair.setVisibility(View.INVISIBLE);
//		mensagem.setVisibility(View.INVISIBLE);
	}
	/*
	 * Vou criar um registo da media dos valores aproximados a 9.81 e -9.81 em cada eixo
	 * 
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {


		int tipoEvento = event.sensor.getType();// guarda o tipo de sensor que gerou o evento
		if(tipoEvento==Sensor.TYPE_ACCELEROMETER) {
		//vai calibrar de acordo com os valores da gravidade, pois este vetor é calibrado.
		


			dadosAcc[0] = event.values[0];
			dadosAcc[1] = event.values[1];
			dadosAcc[2] = event.values[2];
			
			if(startCalib)
			{
				if(contadorProgresso>=LIXO){
					if(!calib.jaCalibrou){
						calib.adicionadaXYZ(dadosAcc);
						
					}
					else
					{
						sensorManager.unregisterListener(this);
						progressBar.setVisibility(View.INVISIBLE);
						//calibText.setText("Calibração efectuada!\nEixo X: "+ calib.getCalibVetor()[0] + "\nEixoY: "+calib.getCalibVetor()[1] + "\nEixoZ: "+calib.getCalibVetor()[2]);
						if(tipoACalibrar=="AccCalibX_high")
						{
							//gravaPreferencias(calib.getCalibVetor()[0],tipoACalibrar);
							xHigh = calib.getCalibVetor()[0];
							mostraDialogo("AccCalibX_low");
						}
						if(tipoACalibrar=="AccCalibX_low")
						{
							//gravaPreferencias(calib.getCalibVetor()[0],tipoACalibrar);
							xLow = calib.getCalibVetor()[0];
							calculoBiasEscala(xHigh,xLow,"X");
							mostraDialogo("AccCalibY_high");
						}

						if(tipoACalibrar=="AccCalibY_high")
						{
							//gravaPreferencias(calib.getCalibVetor()[1],tipoACalibrar);
							yHigh = calib.getCalibVetor()[1];
							mostraDialogo("AccCalibY_low");
						}
						if(tipoACalibrar=="AccCalibY_low")
						{
							//gravaPreferencias(calib.getCalibVetor()[1],tipoACalibrar);
							yLow = calib.getCalibVetor()[1];
							calculoBiasEscala(yHigh,yLow,"Y");
							mostraDialogo("AccCalibZ_high");
						}

						if(tipoACalibrar=="AccCalibZ_high")
						{
							//gravaPreferencias(calib.getCalibVetor()[2],tipoACalibrar);
							zHigh = calib.getCalibVetor()[2];
							mostraDialogo("AccCalibZ_low");
						}
						if(tipoACalibrar=="AccCalibZ_low")
						{
							//gravaPreferencias(calib.getCalibVetor()[2],tipoACalibrar);
							zLow = calib.getCalibVetor()[2];
							calculoBiasEscala(zHigh,zLow,"Z");
							mostraFim();
						}


					}
					

				}
				
				progressBar.setProgress(contadorProgresso);
				contadorProgresso ++;
				
			}
		
		}
		if(tipoEvento==Sensor.TYPE_GRAVITY) {
				sensorText.setText("Progresso:" + contadorProgresso + "\r\nX:" + event.values[0] + "\r\n"+"Y:" + event.values[1] + "\r\n"+"Z:" + event.values[2]);
		}

	}
	public void mostraFim()
	{
		// 9.8 = value * coeficiente
		// coeficiente = 9.8/value
		calibText.setVisibility(View.GONE);
		float X_escala =  prefs.getFloat("k_X", 0);
		float Y_escala = prefs.getFloat("k_Y", 0);
		float Z_escala =  prefs.getFloat("k_Z", 0);
		float X_bias =  prefs.getFloat("b_X", 0);
		float Y_bias = prefs.getFloat("b_Y", 0);
		float Z_bias =  prefs.getFloat("b_Z", 0);

		sensorText.setText("Valores de calibração\r\n" +
				"X_scale:" + Float.toString(X_escala) + "\r\n"+
				"Y_scale:" + Float.toString(Y_escala) + "\r\n"+
				"Z_scale:" + Float.toString(Z_escala) + "\r\n"+
				"X_Bias:" + Float.toString(X_bias) + "\r\n"+
				"Y_Bias:" + Float.toString(Y_bias) + "\r\n"+
				"Z_Bias:" + Float.toString(Z_bias));
	}
	/**
	 * Grava as preferencias em ficheiro.
	 * /data/data/YOUR_PACKAGE_NAME/shared_prefs/YOUR_PREFS_NAME.xml
	 * 
	 * @param dadosCalibrados o valor
	 * @param tipo o tipo que grava x, Y ou Z
	 */
	public void gravaPreferencias(float k,float b,String tipo)
	{
		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat("k_"+tipo, k);
		editor.putFloat("b_"+tipo, b);
		//editor.putFloat(tipoACalibrar, SensorManager.GRAVITY_EARTH /dadosCalibrados);
		//            editor.putFloat("Y", dadosCalibrados);
		//            editor.putFloat("Z", dadosCalibrados);

		editor.commit();



	}
	/***
	 * K = 2 * g / (Mhigh-Mlow)
	 * X = Mlow * K  + g
	 * @param high
	 * @param low
	 * @param tipo
	 */
	public void calculoBiasEscala(float high, float low, String tipo)
	{
		float k_temp = 0;
		float b_temp = 0;
		k_temp = (2*SensorManager.GRAVITY_EARTH) / (high-low);
		b_temp = (low * k_temp) + SensorManager.GRAVITY_EARTH;
		gravaPreferencias(k_temp,b_temp,tipo);
		
	}
	public void mostraDialogo(final String tipo)
	{

		sair.setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				CalibracaoActivity.this.finish();
			}
		});
		calibText.setText("");
		calibrar.setVisibility(View.VISIBLE);
		calib.jaCalibrou = false;
		startCalib = false;
		calib.resetCalib();
		contadorProgresso =0;
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);  
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
				SensorManager.SENSOR_DELAY_FASTEST);   
		if(tipo=="AccCalibX_high")
		{
			//alertbox.setMessage(mensagemX);
			
			mensagem.setText(mensagemX_high);
		}
		if(tipo=="AccCalibX_low")
		{
			//alertbox.setMessage(mensagemX);
			
			mensagem.setText(mensagemX_low);
		}

		if(tipo=="AccCalibY_high")
		{
			//alertbox.setMessage(mensagemY);
			
			mensagem.setText(mensagemY_high);
		}
		if(tipo=="AccCalibY_low")
		{
			//alertbox.setMessage(mensagemY);
			
			mensagem.setText(mensagemY_low);
		}

		if(tipo=="AccCalibZ_high")
		{
			//alertbox.setMessage(mensagemZ);
			
			mensagem.setText(mensagemZ_high);
		}
		if(tipo=="AccCalibZ_low")
		{
			//alertbox.setMessage(mensagemZ);
			
			mensagem.setText(mensagemZ_low);
		}


		// não necesito de clicklisneter para isto?|?|?
		//alertbox.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
		calibrar.setOnClickListener(new View.OnClickListener() {


			@Override
			public void onClick(View v) {
				calibText.setText("A calibrar...");
				startCalib = true;
				progressBar.setVisibility(View.VISIBLE);
				calibText.setVisibility(View.VISIBLE);
				tipoACalibrar = tipo;
				calibrar.setVisibility(View.GONE);
			}
		});

		//alertbox.show();

	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
	@Override
	public void onResume() {
		super.onResume();
		
		calibrar.setVisibility(View.INVISIBLE);
		sair.setVisibility(View.VISIBLE);
		mensagem.setVisibility(View.INVISIBLE);
		//vou verifiar se já temos valores para mostrar
		float X_escala =  prefs.getFloat("k_X", 0);
		float Y_escala = prefs.getFloat("k_Y", 0);
		float Z_escala =  prefs.getFloat("k_Z", 0);
		float X_bias =  prefs.getFloat("b_X", 0);
		float Y_bias = prefs.getFloat("b_Y", 0);
		float Z_bias =  prefs.getFloat("b_Z", 0);

		calibText.setText("Valores de calibração\r\n" +
				"X_scale:" + Float.toString(X_escala) + "\r\n"+
				"Y_scale:" + Float.toString(Y_escala) + "\r\n"+
				"Z_scale:" + Float.toString(Z_escala) + "\r\n"+
				"X_Bias:" + Float.toString(X_bias) + "\r\n"+
				"Y_Bias:" + Float.toString(Y_bias) + "\r\n"+
				"Z_Bias:" + Float.toString(Z_bias));
		continuar.setOnClickListener(new View.OnClickListener() {
		@Override
		public void onClick(View v) {
				continuar.setVisibility(View.INVISIBLE);
				calibrar.setVisibility(View.VISIBLE);
				sair.setVisibility(View.INVISIBLE);
				mensagem.setVisibility(View.VISIBLE);
				
				mostraDialogo("AccCalibX_high");
			}
		});
		sair.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
					finish();
				}
			});
		// Tornar a calibrar
		//mostraFim();
		
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

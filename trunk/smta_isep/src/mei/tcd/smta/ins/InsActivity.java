/**
 * @author pessanha
 * 2012-08-11
 */
package mei.tcd.smta.ins;

import mei.tcd.smta.R;
import mei.tcd.smta.SmtaPreferences;
import mei.tcd.smta.ins.InsListener.OnInsChanged;
import mei.tcd.smta.util.SensorWriterSmta;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
// Implementa o callback criado para o efeito OnInsChanged
public class InsActivity extends Activity implements OnInsChanged{
	// Valor para passar de radianos para graus
	private static final float RAD2GRAUS = 57.2957795f;
	// Tipo de retorno de acordo com o listener no ins
	public enum mTipoRetorno {inicializar,orientacao, velocidade, posicao};
	// A minha views para o INS
	OrientacaoView orientacaoView;
	VelocidadeView velocidadeView;
	CaminhoView caminhoView;
	// Instancia do listener 
	InsListener insListener;
	ProgressDialog dialog;
	// ---------------------------------------- PARA DEBUG DE INFORMAÇAO -----------------------------------
	// referenciar as views
	TextView debugView;
	// Writer de dados
	public SensorWriterSmta sensorWriter = new SensorWriterSmta();
	// ---------------------------------------- FIM DEBUG DE INFORMAÇAO -----------------------------------
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Coloca em fullScreen e deixa ecrã sempre ligado
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.ins);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
		// instancio as minha views
		orientacaoView = (OrientacaoView) this
		.findViewById(R.id.orientacaoView);
		velocidadeView = (VelocidadeView) this
		.findViewById(R.id.velocidadeView);
		caminhoView = (CaminhoView) this
		.findViewById(R.id.caminhoView);
		//debugView = (TextView) findViewById(R.id.debug);
		// Actualizo a orientação a zero
		actualizaOrientacao(new float[] { 0, 0, 0 });
		// Vou buscar as dimensões do ecra e redimensiona as views
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = display.getWidth();  // método deprecated
		//int height = display.getHeight();  // método deprecated
		orientacaoView.getLayoutParams().width = width/2-20;
		velocidadeView.getLayoutParams().width = width/2-20;
		// DEBUG
		//		sensorWriter.criaFicheiro("smta_dados_isep");
		//		sensorWriter.escreveIsto("Azimuth | Pitch | Roll | VelocidadeX | VelocidadeY | VelocidadeZ | VelocidadeToral | PosicaoX | PosicaoY \n");
		// Instancio o meu listener passando o contexto e de onde
		insListener = new InsListener(getApplicationContext(),this);
		// Progress dialog para inicializar e certificar que possuo uma orientação definida
		dialog = new ProgressDialog(this);

	}
	@Override
	public void onResume() {
		super.onResume();
		insListener.start();
		dialog.setCancelable(false);
		dialog.setMessage("A inicializar...");
		dialog.show();
	}

	/**
	 * Actualizo a view de acordo com os parametros Azimuth, Pitch e Roll passados em array
	 * @param apr array Azimuth, pitch e roll
	 */
	private void actualizaOrientacao(float[] apr) {
		float[] copiaApr = new float[3];
		copiaApr = apr.clone();
		// Para passar em graus
		copiaApr[0] *= RAD2GRAUS;
		copiaApr[1] *= RAD2GRAUS;
		copiaApr[2] *= RAD2GRAUS;
		if (orientacaoView != null) {
			orientacaoView.setAzimuth(copiaApr[0]);
			orientacaoView.setPitch(copiaApr[1]);
			orientacaoView.setRoll(copiaApr[2]);
			// Obriga a chamar o onDraw
			orientacaoView.invalidate();
		}

	}
	/**
	 * Actualizo a view velocidade (velocimetro) de acordo com os parametros enviados. 
	 * @param vel A velocidade do objecto
	 * @param semaforo Semaforo que indica se vai a acelarar, abrandar ou parado.
	 */
	private void actualizaVelocidadeView(float vel, int semaforo) {
		if (velocidadeView != null) {
			velocidadeView.setVelocidade(vel);
			velocidadeView.setSemaforo(semaforo);
			velocidadeView.invalidate();
		}
	}
	/**
	 * Actualizo a view velocidade (velocimetro) de acordo com os parametros enviados. 
	 * @param vel A velocidade do objecto
	 * @param semaforo Semaforo que indica se vai a acelarar, abrandar ou parado.
	 */
	private void actualizaPosicaoView(float[] pos) {
		if (caminhoView != null) {
			caminhoView.setPosicao(pos);
			caminhoView.invalidate();
		}
	}

	
	@Override
	public void onStop() {
		super.onStop();
		insListener.stop();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		insListener.stop();
	}

	@Override
	protected void onPause() {
		super.onPause();
		insListener.stop();
	}

	// ------------------------------------------------------------------------
	// Menu de opções
	// Configuração - Configuraçao das preferencias
	// Reset - Reset à velocidade
	// About - Espaço para colocar Acerca
	// ------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.smta_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.config:
			Intent smtaPrefs = new Intent(this, SmtaPreferences.class);
			startActivity(smtaPrefs);
			return true;
		case R.id.reset:
			//ins.velocidade.zero();
			return true;
		case R.id.calibrar:
			Intent smtaCalib = new Intent(this, CalibracaoActivity.class);
			startActivity(smtaCalib);
			return true;

		case R.id.about:

			return true;
		default:
			return super.onOptionsItemSelected(item);
		}

	}
	// Callback do InsListener
	@Override
	public void onInsEvent(Enum value) {
		if(value.name()==mTipoRetorno.orientacao.name())
		{
			actualizaOrientacao(insListener.getAziPitRoll());
		}
		if(value.name()==mTipoRetorno.velocidade.name())
		{
			actualizaVelocidadeView(insListener.getVelocidade(),insListener.getSemaforo());
		}
		if(value.name()==mTipoRetorno.posicao.name())
		{
			actualizaPosicaoView(insListener.getPosicao());
		}
		if(value.name()==mTipoRetorno.inicializar.name())
		{
			if(insListener.getInicio())
			{
				dialog.dismiss();
			}
		}
		
	}
	

}

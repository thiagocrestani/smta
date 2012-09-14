package mei.tcd.smta.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.os.Environment;
import android.util.Log;

public class SensorWriterSmta {
	// Representa caminhos, existentes ou não
	public File ficheiro;
	// Instancia classe FileWriter para escrever ficheiro
	private FileWriter fileWriter;
	
	public void criaFicheiro(String caminho, String nome)
	{
		
		// Criar a pasta usando o objecto File
		File directory = new File(Environment.getExternalStorageDirectory() + "/"+caminho+"/" );
		// Verifica se ela existe, se não existir então cria
		if (!directory.exists())
		{
			Log.v("smta", "A criar directorio.");
			directory.mkdir();
		}
		// Classe Data. vai até ao milisegundo. Java.util
		Date data = new Date();
		// Criação do nome do ficheiro com o objeto FILE que va ser gravado no sdcard. Formatação <ano_mês_dia_hora_minuto_segundo>
		String ficheiroNome = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", data).toString();
		ficheiro = new File(Environment.getExternalStorageDirectory() + "/"+caminho+"/" + nome+ficheiroNome + ".csv");
		try {
			fileWriter = new FileWriter(ficheiro);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v("smta_error", e.getStackTrace().toString());
		}
		// Tenho de colocar a permissão para escrever no storage caso contrário da erro.
	}
	public void escreveIsto(String dados){
		try {
			if (!ficheiro.exists())
			{
				Log.v("smta", "Não existe ficheiro, vou cria-lo.");
				
					ficheiro.createNewFile();
			}
			
			//Log.v("smta", "Estou pronto para escrever!");
			fileWriter.write(dados);
			
//			for(int i=0;i<NUM_OBS;i++){
//				fileWriter.write(vAccel[i] + ";" + vGyro[i] + ";" + vAccelLinear[i] + "\n");
//			}
			//fileWriter.write("\n\n\n" + strRun + "\n");
			//dialog.dismiss();
			//fileWriter.close();
			//btnInicio.performClick();
		} catch (IOException e) {
			
			e.printStackTrace();
			Log.v("smta_error", e.getStackTrace().toString());
			//Toast.makeText(this, "Erro na criação ou escrita do ficheiro.", Toast.LENGTH_LONG).show();
			
		}
	}
	public void fechaFicheiro() 
	{
		try {
			//fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.v("smta_fecho", e.getStackTrace().toString());
		}
		
	}
}

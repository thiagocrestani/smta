package mei.tcd.smta;

import mei.tcd.smta.R;
import mei.tcd.smta.ins.CalibracaoActivity;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Smta_Start extends Activity {
    
	Class classe; // Referenciar as minhas classes ao chamar
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
     // Aceder à Listview passando o ID da listview para a função findViewById
		ListView listView = (ListView) findViewById(R.id.listView1);
		// String de valores para os itens da listview
		
		String[] values = new String[] { "Sensores", "INS/DR","INS/GPS/Google Maps","INS/GPS/Google Logger", "GPS/Google Maps", "INS/GPS/FUSAO/Google Maps"};
		// Instância do objecto ArrayAdapter passando a referencia da actividade usando o this, uma referencia ao textview predefinido e a lista de valores.
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1,  values);
		// Por fim é providenciado ao listView o adaptador com o setAdapter
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				String item = ((TextView)view).getText().toString();
				String strClasse="";
				if(item=="Sensores"){
					strClasse = "sensorinfo.SensorListActivity";
				}
				if(item=="INS/DR"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "ins.InsActivity";
				}
				if(item=="GPS/Google Maps"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "gps.Gps_Activity";
				}
				if(item=="INS/GPS/Google Logger"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "ins.InsGpsLogger";
				}
				if(item=="INS/GPS/FUSAO/Google Maps"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "servicefusion.ServiceActivity";
				}
				if(item=="INS/GPS/Google Maps"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "ins.InsGpsMapsActivity";
				}
				
				try {
					classe = Class.forName("mei.tcd.smta."+strClasse);
					//o this é do context, uma interface implementada pela activity class que proicenciona informações aobre a aplicação
					// O ideal seria colocar o objecto se fosse parceable ou serializable
		    		Intent i = new Intent(Smta_Start.this,classe);
		    		//O startactivity também é do context
		    		startActivity(i);
		    		
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		
			}
			
		});
	
    }
    @Override
	public void onStop()
	{
		super.onStop();
		


	}
    @Override
    protected void onPause() {
        super.onPause();
        
        
       
       
    }
    // Forço o finish da aplicação porque por vezes ela fica a correr em background
    @Override
    protected void onDestroy() {
    	
        super.onDestroy();
        
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(0);
        
        
       
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
		if (item.getItemId() == R.id.config) {
			Intent smtaPrefs = new Intent(this, SmtaPreferences.class);
			startActivity(smtaPrefs);
			return true;
		} else if (item.getItemId() == R.id.reset) {
			//ins.velocidade.zero();
			return true;
		} else if (item.getItemId() == R.id.calibrar) {
			Intent smtaCalib = new Intent(this, CalibracaoActivity.class);
			startActivity(smtaCalib);
			return true;
		} else if (item.getItemId() == R.id.about) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}

	}
}
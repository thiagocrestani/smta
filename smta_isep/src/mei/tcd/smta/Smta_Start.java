package mei.tcd.smta;

import mei.tcd.smta.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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
		
		String[] values = new String[] { "Sensores", "INS / DR"};
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
				if(item=="INS / DR"){
					//strClasse = "ins.Smta_insActivity";
					strClasse = "ins.Smta_INS_Activity";
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
        System.exit(0);
        
        
       
    }
}
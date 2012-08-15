package mei.tcd.smta;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SmtaPreferences extends PreferenceActivity {
	 @Override
	    protected void onCreate(Bundle savedInstanceState) {
	        // TODO Auto-generated method stub
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.preferences);
	    }
}

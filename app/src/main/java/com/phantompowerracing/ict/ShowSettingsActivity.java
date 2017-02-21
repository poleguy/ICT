package com.phantompowerracing.ict;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import java.util.Locale;

public class ShowSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_settings_layout);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        StringBuilder builder = new StringBuilder();
        Intent intent = getIntent();
        int goodReads = intent.getIntExtra("EXTRA_GOOD_READ_COUNT",0);
        int totalReads = intent.getIntExtra("EXTRA_TOTAL_READ_COUNT",0);
        int corruptReads = intent.getIntExtra("EXTRA_CORRUPT_READ_COUNT",0);
        double rps = intent.getDoubleExtra("EXTRA_READS_PER_SECOND",0);

        double lossRate = (double)(totalReads-goodReads)/(double)totalReads*100.0;

        builder.append(sharedPrefs.getBoolean("perform_updates", false)).append("\n");
        builder.append(sharedPrefs.getString("updates_interval", "-1")).append("\n");
        builder.append(sharedPrefs.getString("welcome_message", "NULL")).append("\n");
        builder.append(String.format(Locale.US,"corrupt read count: %d\n", corruptReads));
        builder.append(String.format(Locale.US,"good read count: %d\n", goodReads));
        builder.append(String.format(Locale.US,"total read count: %d\n", totalReads));
        builder.append(String.format(Locale.US,"read failure ratio: %3.3f%%\n",lossRate ));
        builder.append(String.format(Locale.US,"reads per second: %4.3f\n",rps ));

        TextView settingsTextView = (TextView) findViewById(R.id.settings_text_view);
        settingsTextView.setText(builder.toString());

    }

}

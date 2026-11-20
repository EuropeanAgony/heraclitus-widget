package app.heraclitus.daily;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/** Only screen of the app: today's fragment and a button to put the widget on the home screen. */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Fragments.Fragment f = Fragments.today(this);
        ((TextView) findViewById(R.id.label)).setText("FRAGMENT " + f.number);
        ((TextView) findViewById(R.id.quote)).setText(f.quote);
        ((TextView) findViewById(R.id.note)).setText(f.note);

        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, HeraclitusWidget.class);
        Button add = findViewById(R.id.add_widget);
        boolean canPin = manager.isRequestPinAppWidgetSupported();
        add.setVisibility(canPin ? View.VISIBLE : View.GONE);
        add.setOnClickListener(b -> manager.requestPinAppWidget(provider, null, null));

        HeraclitusWidget.updateAll(this);
    }
}

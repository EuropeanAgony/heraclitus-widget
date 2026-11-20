package app.heraclitus.daily;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Home-screen widget: today's fragment; tap to flip to the note and back.
 * Changes at midnight via a non-waking alarm, so it never wakes the phone:
 * the update happens the next time the screen turns on.
 */
public class HeraclitusWidget extends AppWidgetProvider {

    private static final String ACTION_FLIP = "app.heraclitus.daily.FLIP";
    private static final String ACTION_MIDNIGHT = "app.heraclitus.daily.MIDNIGHT";
    private static final String PREFS = "widget_state";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) render(context, manager, id);
        scheduleMidnight(context);
    }

    @Override
    public void onEnabled(Context context) {
        scheduleMidnight(context);
    }

    @Override
    public void onDisabled(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) am.cancel(midnightIntent(context));
    }

    @Override
    public void onDeleted(Context context, int[] ids) {
        SharedPreferences.Editor e = prefs(context).edit();
        for (int id : ids) e.remove("flip_day_" + id);
        e.apply();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_FLIP.equals(action)) {
            int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                SharedPreferences p = prefs(context);
                long today = Fragments.dayNumber();
                boolean flipped = p.getLong("flip_day_" + id, Long.MIN_VALUE) == today;
                if (flipped) p.edit().remove("flip_day_" + id).apply();
                else p.edit().putLong("flip_day_" + id, today).apply();
                render(context, AppWidgetManager.getInstance(context), id);
            }
            return;
        }
        if (ACTION_MIDNIGHT.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            updateAll(context);
            return;
        }
        super.onReceive(context, intent);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, HeraclitusWidget.class));
        for (int id : ids) render(context, manager, id);
        if (ids.length > 0) scheduleMidnight(context);
    }

    private static void render(Context context, AppWidgetManager manager, int id) {
        Fragments.Fragment f = Fragments.today(context);
        // The flip only counts for the day it happened; a new day starts on the quote.
        boolean showNote = prefs(context).getLong("flip_day_" + id, Long.MIN_VALUE) == Fragments.dayNumber();

        RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.widget);
        v.setTextViewText(R.id.label, "FRAGMENT " + f.number);
        v.setTextViewText(R.id.quote, f.quote);
        v.setTextViewText(R.id.note, f.note);
        v.setViewVisibility(R.id.quote, showNote ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.note, showNote ? View.VISIBLE : View.GONE);

        Intent flip = new Intent(context, HeraclitusWidget.class)
                .setAction(ACTION_FLIP)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        PendingIntent pi = PendingIntent.getBroadcast(context, id, flip,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.root, pi);

        manager.updateAppWidget(id, v);
    }

    private static void scheduleMidnight(Context context) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) return;
        long next = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli() + 60_000L;
        // RTC (not RTC_WAKEUP): never wakes the phone; fires when it is next awake.
        am.set(AlarmManager.RTC, next, midnightIntent(context));
    }

    private static PendingIntent midnightIntent(Context context) {
        Intent i = new Intent(context, HeraclitusWidget.class).setAction(ACTION_MIDNIGHT);
        return PendingIntent.getBroadcast(context, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}

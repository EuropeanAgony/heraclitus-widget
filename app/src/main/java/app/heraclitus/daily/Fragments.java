package app.heraclitus.daily;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** The fragments, in daily order. Same order and start date as the laptop lock screen. */
final class Fragments {

    static final LocalDate START = LocalDate.of(2026, 11, 19);

    static final class Fragment {
        final String number, quote, note;
        Fragment(String number, String quote, String note) {
            this.number = number; this.quote = quote; this.note = note;
        }
    }

    private static Fragment[] cache;

    private Fragments() { }

    static synchronized Fragment[] all(Context context) {
        if (cache != null) return cache;
        try (InputStream in = context.getAssets().open("fragments.json")) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int r;
            while ((r = in.read(chunk)) != -1) buf.write(chunk, 0, r);
            JSONArray arr = new JSONArray(new String(buf.toByteArray(), StandardCharsets.UTF_8));
            Fragment[] list = new Fragment[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list[i] = new Fragment(o.getString("n"), o.getString("q"), o.getString("note"));
            }
            cache = list;
            return list;
        } catch (Exception e) {
            return new Fragment[] { new Fragment("41, 42",
                    "You cannot step twice into the same rivers; for fresh waters are ever flowing in upon you.",
                    "The river stays the same river only because new water keeps flowing through it.") };
        }
    }

    static long dayNumber() {
        return ChronoUnit.DAYS.between(START, LocalDate.now());
    }

    static Fragment today(Context context) {
        Fragment[] list = all(context);
        int i = (int) Math.floorMod(dayNumber(), (long) list.length);
        return list[i];
    }
}

package space.aqoleg.statistics;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Data {
    private static final long DAY = 24 * 3600000;
    private static final SimpleDateFormat SDF = new SimpleDateFormat("/yyyy-MM/dd", Locale.getDefault());
    private static JSONArray X;
    private static JSONArray[] Y;
    private static JSONObject NAMES;
    private static JSONObject COLORS;
    private static int length; // X and Y length

    static boolean load(Context context, int dataSetN, long time) {
        String file = String.valueOf(dataSetN);
        if (time == 0 || dataSetN == 4) {
            return loadJson(context, file.concat("/overview.json"));
        } else {
            if (dataSetN < 3) {
                long fileTime = time - DAY * 3;
                if (!loadJson(context, file.concat(SDF.format(new Date(fileTime)).concat(".json")))) {
                    return false;
                }
                for (int i = 0; i < 6; i++) {
                    fileTime += DAY;
                    if (!addJson(context, file.concat(SDF.format(new Date(fileTime)).concat(".json")))) {
                        return false;
                    }
                }
                return true;
            } else {
                return loadJson(
                        context,
                        file.concat(SDF.format(new Date(time)).concat(".json")),
                        file.concat(SDF.format(new Date(time - DAY)).concat(".json")),
                        file.concat(SDF.format(new Date(time - DAY * 7)).concat(".json")),
                        time
                );
            }
        }
    }

    // returns length of the X and Y
    public static int getLength() {
        return length;
    }

    // returns X, 1 >= i > length
    public static long getX(int i) {
        return X.optLong(i);
    }

    // returns Y, 0 >= chartN > charts, 1 >= i > length
    public static int getY(int chartN, int i) {
        return Y[chartN].optInt(i);
    }

    public static String getName(int chartN) {
        return NAMES.optString("y" + chartN);
    }

    public static String getColor(int chartN) {
        return COLORS.optString("y" + chartN);
    }

    private static boolean loadJson(Context context, String file) {
        try {
            InputStream stream = context.getAssets().open(file);
            byte[] buffer = new byte[stream.available()];
            if (stream.read(buffer) != buffer.length) {
                return false;
            }
            JSONObject root = new JSONObject(new String(buffer, "UTF-8"));
            JSONArray columns = root.getJSONArray("columns");
            X = columns.getJSONArray(0);
            int yLength = columns.length() - 1;
            Y = new JSONArray[yLength];
            for (int i = 0; i < yLength; i++) {
                Y[i] = columns.getJSONArray(i + 1);
            }
            NAMES = root.getJSONObject("names");
            COLORS = root.getJSONObject("colors");
            length = X.length();
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private static boolean addJson(Context context, String file) {
        try {
            InputStream stream = context.getAssets().open(file);
            byte[] buffer = new byte[stream.available()];
            if (stream.read(buffer) != buffer.length) {
                return false;
            }
            JSONObject root = new JSONObject(new String(buffer, "UTF-8"));
            JSONArray array = root.getJSONArray("columns").getJSONArray(0);
            int length = array.length();
            for (int i = 1; i < length; i++) {
                X.put(array.optLong(i));
            }
            for (int j = 0; j < Y.length; j++) {
                array = root.getJSONArray("columns").getJSONArray(j + 1);
                for (int i = 1; i < length; i++) {
                    Y[j].put(array.optLong(i));
                }
            }
            Data.length += (length - 1);
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    private static boolean loadJson(Context context, String file0, String file1, String file2, long time) {
        try {
            // file0
            InputStream stream = context.getAssets().open(file0);
            byte[] buffer = new byte[stream.available()];
            if (stream.read(buffer) != buffer.length) {
                return false;
            }
            JSONObject root = new JSONObject(new String(buffer, "UTF-8"));
            JSONArray columns = root.getJSONArray("columns");
            X = columns.getJSONArray(0);
            Y = new JSONArray[3];
            Y[0] = columns.getJSONArray(1);
            COLORS = root.getJSONObject("colors");
            length = X.length();
            // file1
            stream = context.getAssets().open(file1);
            buffer = new byte[stream.available()];
            if (stream.read(buffer) != buffer.length) {
                return false;
            }
            Y[1] = new JSONObject(new String(buffer, "UTF-8")).getJSONArray("columns").getJSONArray(1);
            // file2
            stream = context.getAssets().open(file2);
            buffer = new byte[stream.available()];
            if (stream.read(buffer) != buffer.length) {
                return false;
            }
            Y[2] = new JSONObject(new String(buffer, "UTF-8")).getJSONArray("columns").getJSONArray(1);
            // colors
            COLORS.put("y1", "#558DED");
            COLORS.put("y2", "#5CBCDF");
            // names
            SimpleDateFormat sdf = new SimpleDateFormat(context.getString(R.string.buttonSdf), Locale.getDefault());
            NAMES = new JSONObject();
            NAMES.put("y0", sdf.format(new Date(time)));
            NAMES.put("y1", sdf.format(new Date(time - DAY)));
            NAMES.put("y2", sdf.format(new Date(time - DAY * 7)));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
}
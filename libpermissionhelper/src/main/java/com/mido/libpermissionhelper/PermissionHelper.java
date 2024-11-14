package com.mido.libpermissionhelper;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {
    private static final String SUFFIX = "$$PermissionProxy";

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        doRequestPermission(activity, permissions, requestCode);
    }

    private static void doRequestPermission(Activity activity, String[] permissions, int requestCode) {
        boolean rationale = shouldShowPermissionRationale(activity, permissions, requestCode);
        if (rationale) {
            return;
        }
        Log.d("zhaoming", "_doRequestPermission");
        _doRequestPermission(activity, permissions, requestCode);
    }

    private static void _doRequestPermission(Activity activity, String[] permissions, int requestCode) {
        List<String> deniedPermissions = findDeniedPermissions(activity, permissions);
        if (!deniedPermissions.isEmpty()) {
            String[] denied = new String[deniedPermissions.size()];
            deniedPermissions.toArray(denied);
            ActivityCompat.requestPermissions(activity, denied, requestCode);
        } else {
            doExecuteGrant(activity, permissions, requestCode);
        }
    }

    private static boolean shouldShowPermissionRationale(Activity activity, String[] permissions, int requestCode) {
        PermissionProxy<Activity> proxy = findProxy(activity);
        if (proxy == null) {
            Log.d("zhaoming", "shouldShowPermissionRationale false for proxy == null");
            return false;
        }
        List<String> rationalePermissions = findShouldShowPermissionRationale(activity, permissions);
        if (!rationalePermissions.isEmpty()) {
            String[] rationale = new String[rationalePermissions.size()];
            rationalePermissions.toArray(rationale);
            return proxy.rational(requestCode, activity, rationale, new PermissionRationalCallback() {
                @Override
                public void onRationalExecute() {
                    _doRequestPermission(activity, rationale, requestCode);
                }
            });
        }
        return false;
    }

    private static List<String> findShouldShowPermissionRationale(Activity activity, String[] permissions) {
        List<String> rational = new ArrayList<>();
        boolean result;
        for (String permission: permissions) {
            result = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
            Log.d("zhaoming", permission + " rationale result: " + result);
            if (result) {
                rational.add(permission);
            }
        }
        return rational;
    }

    private static List<String> findDeniedPermissions(Activity activity, String[] permissions) {
        List<String> denied = new ArrayList<>();
        for (String permission: permissions) {
            if (ActivityCompat.checkSelfPermission(activity.getBaseContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                Log.d("zhaoming", "shouldShowRequestPermissionRationale: " + permission);
                denied.add(permission);
            }
        }
        return denied;
    }

    private static void doExecuteGrant(Activity activity, String[] permissions, int requestCode) {
        Log.d("zhaoming", "doExecuteGrant");
        PermissionProxy<Activity> proxy = findProxy(activity);
        if (proxy != null) {
            proxy.grant(requestCode, activity, permissions);
        }
    }

    private static void doExecuteDenied(Activity activity, String[] permissions, int requestCode) {
        Log.d("zhaoming", "doExecuteDenied");
        PermissionProxy<Activity> proxy = findProxy(activity);
        if (proxy != null) {
            proxy.denied(requestCode, activity, permissions);
        }
    }

    private static PermissionProxy findProxy(Activity activity) {
        Class<? extends Activity> aClass = activity.getClass();
        try {
            Class<?> forName = Class.forName(aClass.getName() + SUFFIX);
            return (PermissionProxy) forName.newInstance();
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void onRequestPermissionsResult(Activity activity, int requestCode, String[] permissions, int[] grantResults, int deviceId) {
        List<String> grant = new ArrayList<>();
        List<String> denied = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            String permission = permissions[i];
            Log.d("zhaoming", i + ": " + permission + ", " + grantResults[i]);
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                denied.add(permission);
            } else {
                grant.add(permission);
            }
        }
        if (!grant.isEmpty()) {
            String[] grantArray = new String[grant.size()];
            doExecuteGrant(activity, grant.toArray(grantArray), requestCode);
        }
        if (!denied.isEmpty()) {
            String[] deniedArray = new String[denied.size()];
            doExecuteDenied(activity, denied.toArray(deniedArray), requestCode);
        }
    }
}

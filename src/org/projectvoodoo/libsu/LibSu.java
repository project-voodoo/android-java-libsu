/*
 * Project Voodoo libSu
 * 
 * Author: François SIMOND aka supercurio
 * 
 */

package org.projectvoodoo.libsu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class LibSu {

    Context context;
    String suTestScript = "#!/system/bin/sh\necho ";
    String suTestScriptValid = "SuPermsOkay";

    String TAG = "LibSu";

    // constructors
    public LibSu(Context c) {
        context = c;
    }

    public LibSu(Context c, String tag) {
        context = c;
        TAG = tag;
    }

    public Boolean detectValidSuBinaryInPath() {
        // search for valid su binaries in PATH

        String[] pathToTest = System.getenv("PATH").split(":");

        for (String path : pathToTest) {
            File suBinary = new File(path + "/su");

            if (suBinary.exists()) {
                try {
                    String command = "/system/bin/ls -l " + suBinary.getAbsolutePath();
                    Log.v(TAG, "su listing command: " + command);
                    Process process = Runtime.getRuntime().exec(command);
                    BufferedReader input =
                            new BufferedReader(new InputStreamReader(process.getInputStream()));

                    String line = input.readLine();
                    Log.v(TAG, "su listing output: " + line);
                    if (line != null && line.matches("^-rws.*root.*")) {
                        Log.d(TAG, "Found adequate su binary at " + suBinary.getAbsolutePath());
                        return true;
                    }
                } catch (IOException e) {
                }
            }
        }
        return false;
    }

    public Boolean isSuperUserApkinstalled() {
        try {
            context.getPackageManager().getPackageInfo("com.noshufou.android.su", 0);
            Log.d(TAG, "Superuser.apk com.noshufou.android.su present");
            return true;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    public Boolean isSuAvailable() {
        if (detectValidSuBinaryInPath() && isSuperUserApkinstalled()) {
            return true;
        }

        return false;
    }

    public Boolean canGainSuFor(String commandPath) {
        Process process;
        Boolean result = false;

        try {
            FileOutputStream output = context.openFileOutput(commandPath, Context.MODE_PRIVATE);
            output.write((suTestScript + suTestScriptValid).getBytes());
            output.close();
            String fullCommandPath =
                    context.getFileStreamPath(commandPath).getAbsolutePath();

            // set permissions
            Runtime.getRuntime().exec("chmod 300 " + fullCommandPath);

            // run the command
            String command = "su -c " + fullCommandPath;
            process = Runtime.getRuntime().exec(command);

            // parse output
            String line;
            BufferedReader input =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            while ((line = input.readLine()) != null) {
                if (line.equals(suTestScriptValid))
                    result = true;
            }
            input.close();
            new File(fullCommandPath).delete();

            if (result)
                Log.d(TAG, "Superuser command auth confirmed");
            else
                Log.d(TAG, "Superuser command auth refused");

            return result;

        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return false;
    }
}

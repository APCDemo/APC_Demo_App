/*
     This file is part of the Android app ch.bfh.securevote.
     (C) 2023 Benjamin Fehrensen (and other contributing authors)
     This library is free software; you can redistribute it and/or
     modify it under the terms of the GNU Lesser General Public
     License as published by the Free Software Foundation; either
     version 2.1 of the License, or (at your option) any later version.
     This library is distributed in the hope that it will be useful,
     but WITHOUT ANY WARRANTY; without even the implied warranty of
     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
     Lesser General Public License for more details.
     You should have received a copy of the GNU Lesser General Public
     License along with this library; if not, write to the Free Software
     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/

package ch.bfh.securevote;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.material.snackbar.Snackbar;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;

import ch.bfh.securevote.databinding.ActivityMainBinding;
import ch.bfh.securevote.utils.Constants;
import ch.bfh.securevote.utils.HpcUtility;
import ch.bfh.securevote.utils.SharedData;

import android.view.Menu;
import android.view.MenuItem;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private NavController navController;
    private SharedData sharedData;


    private final NetworkRequest networkRequest = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build();

    /**
     * Check if we are connected. Store state in SharedData.
     */
    private ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            super.onAvailable(network);
            sharedData.setConnected(true);
        }

        @Override
        public void onLost(@NonNull Network network) {
            super.onLost(network);
            sharedData.setConnected(false);
        }

        @Override
        public void onCapabilitiesChanged(@NonNull Network network, @NonNull NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            //final boolean unmetered = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // instantiate shared data
        sharedData = new ViewModelProvider(this).get(SharedData.class);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PreferenceManager.setDefaultValues(this, R.xml.userpreferences, false); //set defaults

        setSupportActionBar(binding.toolbar);

        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        binding.fab.setOnClickListener(view -> {
            if (!composeEmail()) {
                Snackbar.make(view, R.string.no_mail_client_confgured, Snackbar.LENGTH_LONG)
                        .setAction(R.string.no_email_client, null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_check_key){
            navController.navigate(R.id.action_CheckKeyFragment);
            return true;
        }
        else if (id == R.id.action_apc_test){
            navController.navigate(R.id.action_ApcTestFragment);
            return true;
        }
        else if (id == R.id.action_check_key_store){
            navController.navigate(R.id.action_KeyStoreFragment);
            return true;
        }
        else if (id == R.id.action_about){
            Intent urlIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.BACKEND_URL));
            startActivity(urlIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart(){
        super.onStart();
        Log.d(TAG,"Register network callbacks.");
        registerDefaultNetworkCallback();
    }

    @Override
    public void onStop(){
        super.onStop();
        Log.d(TAG,"Unregister network callbacks.");
        unregisterNetworkCallback();
    }

    @Override
    public void onResume(){
        super.onResume();
        checkAccessibilityServices();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    /**
     * Function to check if the device supports biometric authentication with fingerprint reader.
     * @return boolean true or false
     */
    private Boolean checkBiometricSupport() {
        PackageManager packageManager = this.getPackageManager();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.USE_BIOMETRIC) == PackageManager.PERMISSION_GRANTED) {
            return packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        } else {
            notifyUser(getResources().getString(R.string.fingerprint_not_enabled));
        }
        return false;
    }

    private void notifyUser(String message) {
        Toast.makeText(this,
                message,
                Toast.LENGTH_LONG).show();
    }

    /**
     * Check network state. The state is stored in the SharedData.
     * @return boolean true or false
     */
    private boolean checkConnection(@NonNull ConnectivityManager connectivityManager){
        Network network = connectivityManager.getActiveNetwork();
        if (network == null){
            return false;
        }
        else{
            NetworkCapabilities actNw = connectivityManager.getNetworkCapabilities(network);
            return actNw != null
                    && (actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    || actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    || actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
        }
    }

    /**
     * Register for any change in connection state.
     */
    public void registerDefaultNetworkCallback() {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connectivityManager != null;
            connectivityManager.requestNetwork(networkRequest, networkCallback);
            sharedData.setConnected(checkConnection(connectivityManager));
        } catch (Exception e) {
            Log.d("Connection: Exception in registerDefaultNetworkCallback", "xD");
        }
    }

    /**
     * Unregister listener if the Activity is stopped.
     */
    public void unregisterNetworkCallback() {
        if (networkCallback != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            assert connectivityManager != null;
            connectivityManager.bindProcessToNetwork(null);
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
    }

    /**
     * Facilitate users to get in contact per email (pressing the FloatingActionButton).
     * @return true or false
     */
    public boolean composeEmail() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, Constants.emailAddresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, Constants.subject);

        intent.putExtra(Intent.EXTRA_TEXT, HpcUtility.getDeviceInformation());
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
            return true;
        }
        return false;
    }

    public void checkAccessibilityServices(){
        List<AccessibilityServiceInfo> list = null;
        try {
            AccessibilityManager am = (AccessibilityManager) getApplicationContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
            list = am
                    .getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        } catch (Exception ex){
            Log.e(TAG, "Failed to get accessibility services.");
        }
        if (list != null){
            StringBuilder sb = new StringBuilder();
            if (list.size() == 0){
                return;
            } else if (list.size() == 1){
                sb.append(getResources().getString(R.string.one_accessibility_service_running));
                sb.append(getResources().getString(R.string.one_accessibility_will_not_work));
                sb.append(getResources().getString(R.string.please_switch_off));
                sb.append(list.get(0).getId());
                sb.append('.');
            } else if (list.size() > 1){
                sb.append(getResources().getString(R.string.accessibility_services_running));
                sb.append(getResources().getString(R.string.accessibility_services_will_not_work));
                sb.append(getResources().getString(R.string.please_switch_off));
                sb.append(list.get(0).getId());
                for (int i=1; i <= list.size(); i++){
                    sb.append(", ").append(list.get(i).getId());
                }
                sb.append('.');
            }
            new AlertDialog.Builder(MainActivity.this).setMessage(sb.toString()).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            }).create().show();
        }
    }
}
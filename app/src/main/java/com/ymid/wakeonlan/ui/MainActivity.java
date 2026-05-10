package com.ymid.wakeonlan.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.common.collect.Sets;

import java.util.Set;

import com.ymid.wakeonlan.BuildConfig;
import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.ActivityMainBinding;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.shortcuts.DynamicShortcutManager;
import com.ymid.wakeonlan.wear.WearClient;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;

    private WearClient wearClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        // Let the system apply window insets (status bar) to our content
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        // Set status bar color to match toolbar
        getWindow().setStatusBarColor(getResources().getColor(R.color.surfaceVariantColor, getTheme()));

        setContentView(binding.getRoot());

        setVersionInformation();

        setSupportActionBar(binding.toolbar);

        // Ensure app bar respects status bar insets on devices like Samsung S24+
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        initializeNavController();
        initializeWearClient();
        initializeShortcuts();
    }

    private void setVersionInformation() {
        View headerView = binding.navigationView.getHeaderView(0);

        TextView versionView = headerView.findViewById(R.id.navigation_header_version);
        TextView headerTitleView = headerView.findViewById(R.id.navigation_header_title);

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView, (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int extraPadding = getResources().getDimensionPixelSize(R.dimen.navigation_header_top_padding);
            headerView.setPadding(headerView.getPaddingLeft(), topInset + extraPadding,
                    headerView.getPaddingRight(), headerView.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(binding.navigationView);

        versionView.setText(getString(R.string.drawer_menu_header_version, BuildConfig.VERSION_NAME));
    }

    private void initializeWearClient() {
        wearClient = new WearClient(this);
        DeviceRepository.getInstance(this)
                .getAllAsObservable()
                .observe(this, devices -> wearClient.onDeviceListUpdated(devices));
    }

    private void initializeNavController() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(getMenuIds()).setOpenableLayout(binding.drawerLayout).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navigationView, navController);

        setGithubShortcut();
    }

    private void setGithubShortcut() {
        binding.navigationView.getMenu().findItem(R.id.githubShortcut).setOnMenuItemClickListener(item -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/YmidOrtega"));
            startActivity(browserIntent);

            return false;
        });

        binding.navigationView.getMenu().findItem(R.id.settingsActivity).setOnMenuItemClickListener(item -> {
            startActivity(new Intent(this, SettingsActivity.class));
            return false;
        });
    }

    private void initializeShortcuts() {
        DynamicShortcutManager dynamicShortcutManager = new DynamicShortcutManager();
        DeviceRepository.getInstance(this)
                .getAllAsObservable()
                .observe(this, devices -> dynamicShortcutManager.updateShortcuts(this, devices));
    }

    private Set<Integer> getMenuIds() {
        return Sets.newHashSet(R.id.deviceListFragment, R.id.backupFragment, R.id.networkScanFragment);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = binding.drawerLayout;
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}

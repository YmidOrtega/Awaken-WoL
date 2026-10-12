package com.ymid.wakeonlan.ui;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;

import com.ymid.wakeonlan.BuildConfig;
import com.ymid.wakeonlan.R;
import com.ymid.wakeonlan.databinding.ActivityMainBinding;
import com.ymid.wakeonlan.persistence.repository.DeviceRepository;
import com.ymid.wakeonlan.shortcuts.DynamicShortcutManager;
import com.ymid.wakeonlan.ui.onboarding.OnboardingActivity;
import com.ymid.wakeonlan.update.InAppUpdateManager;

public class MainActivity extends AppCompatActivity implements ConsoleHeaderHost {

    private static final String ISSUES_URL = "https://github.com/YmidOrtega/Awaken-Wake-On-Lan-/issues";
    private static final int MORE_BACKUP = 1;
    private static final int MORE_SETTINGS = 2;
    private static final int MORE_ISSUES = 3;

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OnboardingActivity.isComplete(this)) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(binding.getRoot());

        applyWindowInsets();

        LauncherIconManager.ensureValidState(this);
        initializeNavigation();
        initializeShortcuts();
        InAppUpdateManager.attach(this);
        InAppUpdateManager.checkForUpdate(this);
    }

    private void applyWindowInsets() {
        int headerTop = binding.consoleHeader.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(binding.mainRoot, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            binding.consoleHeader.setPadding(binding.consoleHeader.getPaddingLeft(), headerTop + bars.top,
                    binding.consoleHeader.getPaddingRight(), binding.consoleHeader.getPaddingBottom());
            // While typing a command the tabs step aside so the command bar sits right on the keyboard
            boolean imeVisible = ime.bottom > bars.bottom;
            binding.consoleTabs.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            binding.consoleTabsDivider.setVisibility(imeVisible ? View.GONE : View.VISIBLE);
            binding.consoleTabs.setPadding(0, 0, 0, bars.bottom);
            v.setPadding(bars.left, 0, bars.right, imeVisible ? ime.bottom : 0);
            return WindowInsetsCompat.CONSUMED;
        });
    }

    private void initializeNavigation() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);

        binding.tabDevices.setOnClickListener(v -> navigateTo(R.id.deviceListFragment));
        binding.tabScan.setOnClickListener(v -> navigateTo(R.id.networkScanFragment));
        binding.tabHistory.setOnClickListener(v -> navigateTo(R.id.historyFragment));
        binding.tabMore.setOnClickListener(this::showMoreMenu);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> onDestinationChanged(destination));
    }

    private void navigateTo(@IdRes int destinationId) {
        NavDestination current = navController.getCurrentDestination();
        if (current != null && current.getId() == destinationId) {
            return;
        }
        NavOptions options = new NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(navController.getGraph().getStartDestinationId(), false, true)
                .build();
        navController.navigate(destinationId, null, options);
    }

    private void onDestinationChanged(NavDestination destination) {
        int id = destination.getId();
        binding.tabDevices.setSelected(id == R.id.deviceListFragment);
        binding.tabScan.setSelected(id == R.id.networkScanFragment);
        binding.tabHistory.setSelected(id == R.id.historyFragment);
        binding.tabMore.setSelected(id == R.id.backupFragment);

        CharSequence title = id == R.id.deviceListFragment ? getString(R.string.app_name) : destination.getLabel();
        binding.headerTitle.setText(title);
        setHeaderSubtitle(null);
        setHeaderCounter(null);
    }

    private void showMoreMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        Menu menu = popup.getMenu();
        menu.add(Menu.NONE, MORE_BACKUP, 0, R.string.drawer_menu_item_backup);
        menu.add(Menu.NONE, MORE_SETTINGS, 1, R.string.drawer_menu_item_settings);
        menu.add(Menu.NONE, MORE_ISSUES, 2, R.string.drawer_menu_item_github);
        menu.add(Menu.NONE, Menu.NONE, 3, getString(R.string.drawer_menu_header_version, BuildConfig.VERSION_NAME)).setEnabled(false);
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case MORE_BACKUP:
                    navigateTo(R.id.backupFragment);
                    return true;
                case MORE_SETTINGS:
                    startActivity(new Intent(this, SettingsActivity.class));
                    return true;
                case MORE_ISSUES:
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ISSUES_URL)));
                    return true;
                default:
                    return false;
            }
        });
        popup.show();
    }

    @Override
    public void setHeaderSubtitle(@Nullable CharSequence subtitle) {
        TextView view = binding.headerSubtitle;
        view.setText(subtitle);
        view.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
    }

    @Override
    public void setHeaderCounter(@Nullable CharSequence counter) {
        binding.headerCounter.setText(counter);
        binding.headerCounterGroup.setVisibility(counter == null ? View.GONE : View.VISIBLE);
    }

    private void initializeShortcuts() {
        DynamicShortcutManager dynamicShortcutManager = new DynamicShortcutManager();
        DeviceRepository.getInstance(this)
                .getAllAsObservable()
                .observe(this, devices -> dynamicShortcutManager.updateShortcuts(this, devices));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LauncherIconManager.setAppInForeground(this, true);
        InAppUpdateManager.resumeIfUpdateDownloaded(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LauncherIconManager.setAppInForeground(this, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InAppUpdateManager.detach();
    }
}

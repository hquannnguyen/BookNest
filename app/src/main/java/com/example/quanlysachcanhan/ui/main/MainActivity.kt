package com.example.quanlysachcanhan.ui.main

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.databinding.ActivityMainBinding
import com.example.quanlysachcanhan.ui.addedit.AddEditBookActivity
import com.example.quanlysachcanhan.ui.collections.CollectionsFragment
import com.example.quanlysachcanhan.ui.settings.SettingsFragment
import com.example.quanlysachcanhan.ui.stats.StatisticsFragment
import com.example.quanlysachcanhan.utils.LocaleHelper
import com.example.quanlysachcanhan.utils.NavigationHelper
import com.example.quanlysachcanhan.utils.PreferenceManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var preferenceManager: PreferenceManager

    private var activeFragment: Fragment? = null
    var currentTab: Int = TAB_LIBRARY
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        preferenceManager = PreferenceManager(this)
        AppCompatDelegate.setDefaultNightMode(preferenceManager.nightMode)

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTopHeader()
        setupFragments(savedInstanceState)
        setupBottomNavigation()
        setupFab()
        setupBackNavigation()

        if (savedInstanceState == null) {
            val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, TAB_LIBRARY)
            if (selectedTab != TAB_LIBRARY) {
                selectTab(selectedTab)
            }
        } else {
            updateBottomNavUI(currentTab)
            if (currentTab == TAB_LIBRARY) {
                binding.fabAddBook.show()
            } else {
                binding.fabAddBook.hide()
            }
        }

        if (intent.getBooleanExtra(NavigationHelper.EXTRA_OPEN_FILTER, false)) {
            (supportFragmentManager.findFragmentByTag(TAG_LIBRARY) as? LibraryFragment)?.openFilterDialog()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CURRENT_TAB, currentTab)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val selectedTab = intent.getIntExtra(EXTRA_SELECTED_TAB, -1)
        if (selectedTab != -1) {
            selectTab(selectedTab)
        }
        if (intent.getBooleanExtra(NavigationHelper.EXTRA_OPEN_FILTER, false)) {
            selectTab(TAB_LIBRARY)
            (supportFragmentManager.findFragmentByTag(TAG_LIBRARY) as? LibraryFragment)?.openFilterDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateHeaderUI()
    }

    private fun setupTopHeader() {
        updateHeaderUI()

        binding.btnLanguage.setOnClickListener {
            NavigationHelper.showLanguageDialog(this)
        }

        binding.btnReminder.setOnClickListener {
            NavigationHelper.showNotificationCenterDialog(this, preferenceManager)
        }
    }

    private fun updateHeaderUI() {
        if (LocaleHelper.isVietnamese()) {
            binding.ivLangFlag.setImageResource(R.drawable.ic_flag_vn)
            binding.tvLangCode.text = "VI"
        } else {
            binding.ivLangFlag.setImageResource(R.drawable.ic_flag_en)
            binding.tvLangCode.text = "EN"
        }

        binding.viewReminderDot.visibility =
            if (preferenceManager.reminderEnabled) View.VISIBLE else View.GONE
    }

    private fun setupFragments(savedInstanceState: Bundle?) {
        val fm = supportFragmentManager
        currentTab = savedInstanceState?.getInt(KEY_CURRENT_TAB, TAB_LIBRARY) ?: TAB_LIBRARY

        var lib = fm.findFragmentByTag(TAG_LIBRARY)
        var col = fm.findFragmentByTag(TAG_COLLECTIONS)
        var stat = fm.findFragmentByTag(TAG_STATISTICS)
        var set = fm.findFragmentByTag(TAG_SETTINGS)

        val tx = fm.beginTransaction()

        if (lib == null) {
            lib = LibraryFragment.newInstance()
            tx.add(R.id.mainFragmentContainer, lib, TAG_LIBRARY)
        }
        if (col == null) {
            col = CollectionsFragment.newInstance()
            tx.add(R.id.mainFragmentContainer, col, TAG_COLLECTIONS)
        }
        if (stat == null) {
            stat = StatisticsFragment.newInstance()
            tx.add(R.id.mainFragmentContainer, stat, TAG_STATISTICS)
        }
        if (set == null) {
            set = SettingsFragment.newInstance()
            tx.add(R.id.mainFragmentContainer, set, TAG_SETTINGS)
        }

        val target = when (currentTab) {
            TAB_COLLECTIONS -> col
            TAB_STATISTICS -> stat
            TAB_SETTINGS -> set
            else -> lib
        }

        listOf(lib, col, stat, set).forEach { f ->
            if (f == target) {
                tx.show(f)
                tx.setMaxLifecycle(f, Lifecycle.State.RESUMED)
            } else {
                tx.hide(f)
                tx.setMaxLifecycle(f, Lifecycle.State.STARTED)
            }
        }

        tx.commitAllowingStateLoss()
        activeFragment = target
    }

    private fun setupBottomNavigation() {
        binding.navLibrary.setOnClickListener {
            selectTab(TAB_LIBRARY)
        }
        binding.navCollections.setOnClickListener {
            selectTab(TAB_COLLECTIONS)
        }
        binding.navStatistics.setOnClickListener {
            selectTab(TAB_STATISTICS)
        }
        binding.navSettings.setOnClickListener {
            selectTab(TAB_SETTINGS)
        }

        updateBottomNavUI(currentTab)
    }

    private fun setupFab() {
        binding.fabAddBook.setOnClickListener {
            NavigationHelper.startActivitySlide(this, Intent(this, AddEditBookActivity::class.java))
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this) {
            if (currentTab != TAB_LIBRARY) {
                selectTab(TAB_LIBRARY)
            } else {
                finish()
            }
        }
    }

    fun selectTab(tabIndex: Int) {
        val fm = supportFragmentManager
        if (fm.isStateSaved) return

        val targetTag = when (tabIndex) {
            TAB_COLLECTIONS -> TAG_COLLECTIONS
            TAB_STATISTICS -> TAG_STATISTICS
            TAB_SETTINGS -> TAG_SETTINGS
            else -> TAG_LIBRARY
        }

        var targetFragment = fm.findFragmentByTag(targetTag)
        val tx = fm.beginTransaction()

        if (targetFragment == null) {
            targetFragment = when (tabIndex) {
                TAB_COLLECTIONS -> CollectionsFragment.newInstance()
                TAB_STATISTICS -> StatisticsFragment.newInstance()
                TAB_SETTINGS -> SettingsFragment.newInstance()
                else -> LibraryFragment.newInstance()
            }
            tx.add(R.id.mainFragmentContainer, targetFragment, targetTag)
        }

        // Hiển thị/ẩn nút FAB thêm sách
        if (tabIndex == TAB_LIBRARY) {
            binding.fabAddBook.show()
        } else {
            binding.fabAddBook.hide()
        }

        if (activeFragment != targetFragment || !targetFragment.isVisible) {
            activeFragment?.let { prev ->
                if (prev != targetFragment && prev.isAdded) {
                    tx.hide(prev)
                    tx.setMaxLifecycle(prev, Lifecycle.State.STARTED)
                }
            }

            // Đảm bảo các tab khác được ẩn an toàn
            listOf(TAG_LIBRARY, TAG_COLLECTIONS, TAG_STATISTICS, TAG_SETTINGS).forEach { tag ->
                if (tag != targetTag) {
                    fm.findFragmentByTag(tag)?.let { other ->
                        if (other.isAdded && other != activeFragment) {
                            tx.hide(other)
                            tx.setMaxLifecycle(other, Lifecycle.State.STARTED)
                        }
                    }
                }
            }

            tx.show(targetFragment)
            tx.setMaxLifecycle(targetFragment, Lifecycle.State.RESUMED)
            tx.commitAllowingStateLoss()

            activeFragment = targetFragment
            currentTab = tabIndex
        } else if (tabIndex == TAB_LIBRARY) {
            (targetFragment as? LibraryFragment)?.scrollToTop()
        }

        updateBottomNavUI(tabIndex)
    }

    private fun updateBottomNavUI(activeTab: Int) {
        val activeColor = ContextCompat.getColor(this, R.color.secondary_oak)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_secondary)

        fun applyTabState(
            icon: ImageView,
            text: TextView,
            isActive: Boolean
        ) {
            val color = if (isActive) activeColor else inactiveColor
            icon.setColorFilter(color)
            text.setTextColor(color)
            text.setTypeface(null, if (isActive) Typeface.BOLD else Typeface.NORMAL)
        }

        applyTabState(binding.iconNavLibrary, binding.textNavLibrary, activeTab == TAB_LIBRARY)
        applyTabState(binding.iconNavCollections, binding.textNavCollections, activeTab == TAB_COLLECTIONS)
        applyTabState(binding.iconNavStatistics, binding.textNavStatistics, activeTab == TAB_STATISTICS)
        applyTabState(binding.iconNavSettings, binding.textNavSettings, activeTab == TAB_SETTINGS)
    }

    companion object {
        const val TAB_LIBRARY = 0
        const val TAB_COLLECTIONS = 1
        const val TAB_STATISTICS = 2
        const val TAB_SETTINGS = 3

        const val EXTRA_SELECTED_TAB = "EXTRA_SELECTED_TAB"
        private const val KEY_CURRENT_TAB = "KEY_CURRENT_TAB"

        private const val TAG_LIBRARY = "TAG_LIBRARY"
        private const val TAG_COLLECTIONS = "TAG_COLLECTIONS"
        private const val TAG_STATISTICS = "TAG_STATISTICS"
        private const val TAG_SETTINGS = "TAG_SETTINGS"
    }
}
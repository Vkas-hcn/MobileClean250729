package com.be.poles.apart.mobileclean.main

import android.Manifest
import android.app.usage.StorageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.provider.Settings
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.be.poles.apart.mobileclean.CleanActivity
import com.be.poles.apart.mobileclean.FileScanActivity
import com.be.poles.apart.mobileclean.NetActivity
import com.be.poles.apart.mobileclean.PicCleanActivity
import com.be.poles.apart.mobileclean.R
import com.be.poles.apart.mobileclean.databinding.ActivityMainBinding
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {
    private var isForPicClean = false
    private var isForFileClean = false

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val decimalFormat = DecimalFormat("#.#")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            when {
                isForPicClean -> startPicCleanActivity()
                isForFileClean -> startFileCleanActivity()
                else -> startCleanActivity()
            }
        } else {
            showSettingsDialog()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                when {
                    isForPicClean -> startPicCleanActivity()
                    isForFileClean -> startFileCleanActivity()
                    else -> startCleanActivity()
                }
            } else {
                showSettingsDialog()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        updateStorageInfo()
    }

    private fun initViews() {
        binding.tvClean.setOnClickListener {
            checkPermissionsAndStartClean()
        }

        binding.tvCancel.setOnClickListener {
            hideDialog()
        }

        binding.tvYes.setOnClickListener {
            hideDialog()
            requestStoragePermissionsForPicClean()
        }

        binding.imgSetting.setOnClickListener {
            startActivity(Intent(this, NetActivity::class.java))
        }

        binding.llPicture.setOnClickListener {
            checkPermissionsAndStartPicClean()
        }

        binding.llFile.setOnClickListener {
            checkPermissionsAndStartFileClean()
        }
    }
    private fun getTotalStorageSpace(): Long {
        return try {
            val storageStatsManager =
                getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT
            storageStatsManager.getTotalBytes(uuid)
        } catch (e: Exception) {
            e.printStackTrace()
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.blockCountLong * stat.blockSizeLong
        }
    }


    private fun getAvailableStorageSpace(): Long {
        return try {
            val storageStatsManager =
                getSystemService(STORAGE_STATS_SERVICE) as StorageStatsManager
            val uuid = StorageManager.UUID_DEFAULT
            storageStatsManager.getFreeBytes(uuid)
        } catch (e: Exception) {
            e.printStackTrace()
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        }
    }
    private fun updateStorageInfo() {
        try {
            val totalSpace = getTotalStorageSpace()
            val availableSpace = getAvailableStorageSpace()
            val usedSpace = totalSpace - availableSpace
            updateStorageDisplay(availableSpace, usedSpace, totalSpace)

        } catch (e: Exception) {
            e.printStackTrace()
            try {
                val dataDir = Environment.getDataDirectory()
                val stat = StatFs(dataDir.path)
                val totalSpace = stat.blockCountLong * stat.blockSizeLong
                val availableSpace = stat.availableBlocksLong * stat.blockSizeLong
                val usedSpace = totalSpace - availableSpace

                updateStorageDisplay(availableSpace, usedSpace, totalSpace)
            } catch (e2: Exception) {
                e2.printStackTrace()
                binding.tvFreeNum.text = "0"
                binding.tvFreeUn.text = "GB"
                binding.tvUserNum.text = "0"
                binding.tvUserUn.text = "GB"
                binding.tvProgressNum.text = "0"
                binding.pc.progress = 0
            }
        }
    }

    private fun updateStorageDisplay(availableSpace: Long, usedSpace: Long, totalSpace: Long) {
        val (freeSize, freeUnit) = formatStorage(availableSpace)
        binding.tvFreeNum.text = freeSize
        binding.tvFreeUn.text = freeUnit

        val (usedSize, usedUnit) = formatStorage(usedSpace)
        binding.tvUserNum.text = usedSize
        binding.tvUserUn.text = usedUnit

        val usagePercentage = if (totalSpace > 0) {
            ((usedSpace.toDouble() / totalSpace.toDouble()) * 100).toInt()
        } else {
            0
        }

        binding.tvProgressNum.text = usagePercentage.toString()
        binding.pc.progress = usagePercentage
    }

    private fun formatStorage(bytes: Long): Pair<String, String> {
        return when {
            bytes >= 1000 * 1000 * 1000 -> {
                val gb = bytes.toDouble() / (1000 * 1000 * 1000)
                Pair(decimalFormat.format(gb), "GB")
            }
            bytes >= 1000 * 1000 -> {
                val mb = bytes.toDouble() / (1000 * 1000)
                Pair(decimalFormat.format(mb), "MB")
            }
            else -> {
                val kb = bytes.toDouble() / 1000
                Pair(decimalFormat.format(kb), "KB")
            }
        }
    }

    private fun checkPermissionsAndStartClean() {
        isForPicClean = false
        isForFileClean = false
        if (hasStoragePermissions()) {
            startCleanActivity()
        } else {
            showDialog()
        }
    }

    private fun checkPermissionsAndStartPicClean() {
        isForPicClean = true
        isForFileClean = false
        if (hasStoragePermissions()) {
            startPicCleanActivity()
        } else {
            showDialog()
        }
    }

    private fun checkPermissionsAndStartFileClean() {
        isForPicClean = false
        isForFileClean = true
        if (hasStoragePermissions()) {
            startFileCleanActivity()
        } else {
            showDialog()
        }
    }

    private fun hasStoragePermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestStoragePermissionsForPicClean() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = "package:$packageName".toUri()
                manageStorageLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                manageStorageLauncher.launch(intent)
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            requestPermissionLauncher.launch(permissions)
        }
    }
    private fun showDialog() {
        binding.llDialog.visibility = View.VISIBLE
    }

    private fun hideDialog() {
        binding.llDialog.visibility = View.GONE
    }

    private fun showSettingsDialog() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = "package:$packageName".toUri()
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startCleanActivity() {
        val intent = Intent(this, CleanActivity::class.java)
        startActivity(intent)
    }

    private fun startPicCleanActivity() {
        val intent = Intent(this, PicCleanActivity::class.java)
        startActivity(intent)
    }

    private fun startFileCleanActivity() {
        val intent = Intent(this, FileScanActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateStorageInfo()
    }
}
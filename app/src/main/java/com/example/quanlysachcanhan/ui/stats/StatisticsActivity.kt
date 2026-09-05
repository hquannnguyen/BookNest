package com.example.quanlysachcanhan.ui.stats

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysachcanhan.R
import com.example.quanlysachcanhan.data.BookRepository
import com.example.quanlysachcanhan.databinding.ActivityStatisticsBinding
import com.example.quanlysachcanhan.utils.Constants
import java.util.Calendar

class StatisticsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStatisticsBinding
    private lateinit var repository: BookRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStatisticsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = BookRepository(this)
        loadStatistics()
    }

    private fun loadStatistics() {
        val year = Calendar.getInstance().get(Calendar.YEAR)

        binding.tvTotal.text = repository.countAll().toString()
        binding.tvUnread.text =
            repository.countByStatus(Constants.ReadingStatus.UNREAD).toString()
        binding.tvReading.text =
            repository.countByStatus(Constants.ReadingStatus.READING).toString()
        binding.tvRead.text =
            repository.countByStatus(Constants.ReadingStatus.READ).toString()

        binding.tvReadInYear.text =
            repository.countReadBooksInYear(year).toString()

        binding.tvYearLabel.text = getString(R.string.label_read_in_year, year)

        // TODO PhongVV - STATISTICS:
        // - Bieu do theo the loai (Constants.Category.ALL)
        // - Bieu do theo trang thai doc
        // Co the dung ProgressBar hoac MPAndroidChart.
    }
}
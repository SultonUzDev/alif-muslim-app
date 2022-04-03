package id.derysudrajat.alif.ui.activity

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import dagger.hilt.android.AndroidEntryPoint
import id.derysudrajat.alif.R
import id.derysudrajat.alif.data.model.ProgressTask
import id.derysudrajat.alif.databinding.ActivityProgressBinding
import id.derysudrajat.alif.ui.addactivity.AddProgressActivity
import id.derysudrajat.alif.utils.TimeUtils.fullDate
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class ProgressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProgressBinding
    private val viewModel: ProgressActivityViewModel by viewModels()
    private val scope = lifecycleScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        viewModel.getTodayActivity()
        setupAppBar()
        onSwipeDelete()
        scope.launch { viewModel.activities.collect(this@ProgressActivity::populateActivities) }

        binding.nestedScrollView.setOnScrollChangeListener(scrollListener)
        binding.btnAddActivity.setOnClickListener {
            startForResult.launch(Intent(this, AddProgressActivity::class.java))
        }
        binding.tvDate.text = Timestamp.now().fullDate
    }

    private fun populateActivities(activities: List<ProgressTask>) {
        if (activities.isNotEmpty()) populateHeader(activities)

        binding.rvActivity.apply {
            itemAnimator = DefaultItemAnimator()
            adapter = ActivityAdapter(activities) { id, isChecked ->
                viewModel.checkedTask(id, isChecked)
            }
        }
    }

    private fun populateHeader(activities: List<ProgressTask>) {
        binding.tvTotalTask.text = buildString { append("All(${activities.size})") }
        val progress = activities.filter { it.isCheck }.size.toDouble()
        val percentage = (progress / activities.size.toDouble()) * 100
        binding.tvProgress.text = buildString {
            if (percentage % 2.0 == 0.0) append(percentage.toInt())
            else append(DecimalFormat("##.##").format(percentage))
            append("%")
        }
        binding.linearProgressIndicator.apply {
            setProgress(progress.toInt())
            max = activities.size
        }
    }

    private val scrollListener = NestedScrollView.OnScrollChangeListener { _, _, y, _, oldY ->
        if (y > oldY) binding.btnAddActivity.shrink() else binding.btnAddActivity.extend()
    }

    private fun setupAppBar() = binding.appBar.apply {
        tvTitle.text = "Activity"
        btnBack.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        setResult(RESULT_OK)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) viewModel.getTodayActivity()
        }

    private fun onSwipeDelete() {
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(v: RecyclerView, h: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(h: RecyclerView.ViewHolder, dir: Int) {
                viewModel.deleteTask(h.absoluteAdapterPosition)
            }
        }).attachToRecyclerView(binding.rvActivity)
    }
}
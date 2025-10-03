package com.smartacademictracker.presentation.common

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.smartacademictracker.data.model.GradePeriod

object ChartUtils {
    
    @Composable
    fun GradeTrendChart(
        prelimGrade: Double?,
        midtermGrade: Double?,
        finalGrade: Double?,
        modifier: Modifier = Modifier
    ) {
        val entries = mutableListOf<Entry>()
        val labels = mutableListOf<String>()
        
        // Add entries for each period that has a grade
        var index = 0f
        if (prelimGrade != null) {
            entries.add(Entry(index, prelimGrade.toFloat()))
            labels.add(GradePeriod.PRELIM.displayName)
            index++
        }
        if (midtermGrade != null) {
            entries.add(Entry(index, midtermGrade.toFloat()))
            labels.add(GradePeriod.MIDTERM.displayName)
            index++
        }
        if (finalGrade != null) {
            entries.add(Entry(index, finalGrade.toFloat()))
            labels.add(GradePeriod.FINAL.displayName)
            index++
        }
        
        if (entries.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupGradeTrendChart(entries, labels)
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        } else {
            // Show empty state
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text("No grades available")
            }
        }
    }
    
    @Composable
    fun SubjectComparisonChart(
        subjects: List<Pair<String, Double>>, // Subject name and average grade
        modifier: Modifier = Modifier
    ) {
        if (subjects.isNotEmpty()) {
            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setupSubjectComparisonChart(subjects)
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp)
            )
        } else {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text("No subject data available")
            }
        }
    }
    
    private fun LineChart.setupGradeTrendChart(entries: List<Entry>, labels: List<String>) {
        val dataSet = LineDataSet(entries, "Grade Trend").apply {
            color = Color(0xFF2196F3).toArgb()
            setCircleColor(Color(0xFF2196F3).toArgb())
            lineWidth = 3f
            circleRadius = 6f
            setDrawFilled(true)
            fillColor = Color(0xFF2196F3).copy(alpha = 0.3f).toArgb()
            valueTextSize = 12f
            valueTextColor = Color.Black.toArgb()
        }
        
        val lineData = LineData(dataSet)
        data = lineData
        
        // Configure chart appearance
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        
        // Configure axes
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index < labels.size) labels[index] else ""
                }
            }
        }
        
        axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = 0f
            axisMaximum = 100f
            granularity = 10f
        }
        
        axisRight.isEnabled = false
        
        // Animate chart
        animateX(1000)
        animateY(1000)
        
        invalidate()
    }
    
    private fun LineChart.setupSubjectComparisonChart(subjects: List<Pair<String, Double>>) {
        val entries = subjects.mapIndexed { index, (_, grade) ->
            Entry(index.toFloat(), grade.toFloat())
        }
        
        val dataSet = LineDataSet(entries, "Subject Performance").apply {
            color = Color(0xFF4CAF50).toArgb()
            setCircleColor(Color(0xFF4CAF50).toArgb())
            lineWidth = 3f
            circleRadius = 6f
            setDrawFilled(true)
            fillColor = Color(0xFF4CAF50).copy(alpha = 0.3f).toArgb()
            valueTextSize = 12f
            valueTextColor = Color.Black.toArgb()
        }
        
        val lineData = LineData(dataSet)
        data = lineData
        
        // Configure chart appearance
        description.isEnabled = false
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        
        // Configure axes
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            setDrawGridLines(false)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val index = value.toInt()
                    return if (index < subjects.size) subjects[index].first else ""
                }
            }
        }
        
        axisLeft.apply {
            setDrawGridLines(true)
            axisMinimum = 0f
            axisMaximum = 100f
            granularity = 10f
        }
        
        axisRight.isEnabled = false
        
        // Animate chart
        animateX(1000)
        animateY(1000)
        
        invalidate()
    }
}

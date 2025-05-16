package com.example.stockapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import okhttp3.*
import java.io.IOException

class StockInformation : AppCompatActivity() {

    private lateinit var companyName: TextView
    private lateinit var assetType: TextView
    private lateinit var exchange: TextView
    private lateinit var price: TextView
    private lateinit var change: TextView

    private lateinit var about: TextView
    private lateinit var aboutTitle: TextView
    private lateinit var industry: TextView
    private lateinit var sector: TextView

    private lateinit var low52: TextView
    private lateinit var currPrice: TextView
    private lateinit var high52: TextView

    private lateinit var marketCap: TextView
    private lateinit var peRatio: TextView
    private lateinit var divYield: TextView
    private lateinit var beta: TextView
    private lateinit var profitMargin: TextView

    private lateinit var lineChart: LineChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        companyName = findViewById(R.id.tvCompanyName)
        assetType = findViewById(R.id.tvSymbol)
        exchange = findViewById(R.id.tvExchange)
        price = findViewById(R.id.tvPrice)
        change = findViewById(R.id.tvChange)

        aboutTitle = findViewById(R.id.aboutTitle)
        about = findViewById(R.id.tvAbout)
        industry = findViewById(R.id.industry)
        sector = findViewById(R.id.sector)

        low52 = findViewById(R.id.low52)
        currPrice = findViewById(R.id.currPrice)
        high52 = findViewById(R.id.high52)

        marketCap = findViewById(R.id.marketCap)
        peRatio = findViewById(R.id.peRatio)
        divYield = findViewById(R.id.divYield)
        beta = findViewById(R.id.beta)
        profitMargin = findViewById(R.id.profitMargin)

        val apiKey = "C34A4OLXI65MG5IS"
        val symbol = intent.getStringExtra("symbol") ?: ""
        val pricePassed = intent.getStringExtra("price") ?: ""
        val changePassed = intent.getStringExtra("change") ?: ""

        price.text = "$$pricePassed"
        change.text = changePassed

        lifecycleScope.launch {
            getData(symbol, apiKey)
        }

        lineChart = findViewById(R.id.lineChart)
        fetchDataAndPlot(symbol,apiKey)
    }

    private fun fetchDataAndPlot(symbol:String,apiKey:String) {
        val url = "https://www.alphavantage.co/query?function=TIME_SERIES_WEEKLY&symbol=$symbol&apikey=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let {
                    val jsonObject = JSONObject(it)
                    val timeSeries = jsonObject.getJSONObject("Weekly Time Series")

                    val entries = ArrayList<Entry>()
                    val labels = ArrayList<String>()
                    var index = 0f

                    val sortedDates = timeSeries.keys().asSequence().toList().sorted()

                    for (date in sortedDates) {
                        val dataPoint = timeSeries.getJSONObject(date)
                        val close = dataPoint.getDouble("4. close").toFloat()
                        entries.add(Entry(index, close))
                        labels.add(date)
                        index++
                    }

                    runOnUiThread {
                        val dataSet = LineDataSet(entries, "$symbol Weekly Close")
                        dataSet.color = getColor(R.color.blue)
                        dataSet.setCircleColor(getColor(R.color.blue))
                        val lineData = LineData(dataSet)

                        lineChart.data = lineData
                        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
                        lineChart.xAxis.setDrawLabels(true)
                        lineChart.axisRight.isEnabled = false
                        lineChart.description.text = "Weekly Closing Price"
                        lineChart.invalidate()
                    }
                }
            }
        })
    }

    private suspend fun getData(symbol: String, apiKey: String) = withContext(Dispatchers.IO) {
        val url = "https://www.alphavantage.co/query?function=OVERVIEW&symbol=$symbol&apikey=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext

        val body = response.body?.string() ?: return@withContext
        val json = JSONObject(body)

        val name = json.optString("Name", "")
        val type = json.optString("AssetType", "")
        val exch = json.optString("Exchange", "")
        val desc = json.optString("Description", "")
        val sectorText = json.optString("Sector", "")
        val industryText = json.optString("Industry", "")

        val low = json.optString("52WeekLow", "")
        val high = json.optString("52WeekHigh", "")
        val cap = json.optString("MarketCapitalization", "")
        val pe = json.optString("PERatio", "")
        val yield = json.optString("DividendYield", "")
        val betaVal = json.optString("Beta", "")
        val margin = json.optString("ProfitMargin", "")

        withContext(Dispatchers.Main) {
            companyName.text = name
            assetType.text = "$symbol, $type"
            exchange.text = exch

            about.text = desc
            aboutTitle.text = "About $name"
            sector.text = "Sector: $sectorText"
            industry.text = "Industry: $industryText"

            low52.text = "52-Week Low\n$$low"
            high52.text = "52-High Low\n$$high"
            currPrice.text = "Current Price\n"+price.text

            marketCap.text = "Market Cap\n$${formatLargeNumber(cap)}"
            peRatio.text = "P/E Ratio\n$pe"
            divYield.text = "Dividend Yield\n${(yield.toDoubleOrNull() ?: 0.0) * 100}%"
            beta.text = "Beta\n$betaVal"
            profitMargin.text = "Profit Margin\n$margin"
        }
    }

    private fun formatLargeNumber(value: String): String {
        return try {
            val num = value.toDouble()
            when {
                num >= 1_000_000_000_000 -> "%.2fT".format(num / 1_000_000_000_000)
                num >= 1_000_000_000 -> "%.2fB".format(num / 1_000_000_000)
                num >= 1_000_000 -> "%.2fM".format(num / 1_000_000)
                else -> num.toString()
            }
        } catch (e: Exception) {
            value
        }
    }
}
package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val api_key="demo"
        val symbol="Stock_name"
        val stockInfoApi="https://www.alphavantage.co/query?function=OVERVIEW&symbol=$symbol&apikey=C34A4OLXI65MG5IS"


        val etSearch = findViewById<AutoCompleteTextView>(R.id.etSearch)
        val suggestionAdapter = ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line)
        val suggestionList = mutableListOf<SymbolSuggestion>()

        etSearch.setAdapter(suggestionAdapter)

        etSearch.threshold = 1 // Start suggesting from 1 letter

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.length >= 3) {
                    fetchSuggestions(query, api_key) { suggestions ->
                        runOnUiThread {
                            suggestionList.clear()
                            suggestionList.addAll(suggestions)
                            val displayList = suggestions.map { "${it.symbol} - ${it.name}" }
                            suggestionAdapter.clear()
                            suggestionAdapter.addAll(displayList)
                            suggestionAdapter.notifyDataSetChanged()
                            etSearch.showDropDown()
                        }
                    }
                }
            }
        })

        etSearch.setOnItemClickListener { _, _, position, _ ->
            val selected = suggestionList[position]
            Toast.makeText(this, "Selected: ${selected.symbol}", Toast.LENGTH_SHORT).show()
            etSearch.setText(selected.symbol)
        }

        lifecycleScope.launch(Dispatchers.IO) {

            val gainersAll = getTopGainers(api_key)
            val losersAll = getTopLosers(api_key)

            val gainers = gainersAll.take(4)
            val losers = losersAll.take(4)

            withContext(Dispatchers.Main) {
                val gainersAdapter = StockAdapter(gainers) { stock ->
                    Toast.makeText(this@MainActivity, "Gainer clicked: ${stock.symbol}", Toast.LENGTH_SHORT).show()
                    val i=Intent(this@MainActivity,StockInformation::class.java)
                    i.putExtra("symbol",stock.symbol)
                    i.putExtra("price",stock.price)
                    i.putExtra("change",stock.change)
                    startActivity(i)
                }
                val losersAdapter = StockAdapter(losers) { stock ->
                    Toast.makeText(this@MainActivity, "Loser clicked: ${stock.symbol}", Toast.LENGTH_SHORT).show()
                    val i=Intent(this@MainActivity,StockInformation::class.java)
                    i.putExtra("symbol",stock.symbol)
                    i.putExtra("price",stock.price)
                    i.putExtra("change",stock.change)
                    startActivity(i)
                }

                findViewById<RecyclerView>(R.id.rvTopGainers).apply {
                    layoutManager = GridLayoutManager(this@MainActivity, 2)
                    adapter = gainersAdapter
                }

                findViewById<RecyclerView>(R.id.rvTopLosers).apply {
                    layoutManager = GridLayoutManager(this@MainActivity, 2)
                    adapter = losersAdapter
                }
            }

            val viewAllGainer=findViewById<TextView>(R.id.tvTopGainersViewAll)
            val viewAllLoser=findViewById<TextView>(R.id.tvTopLosersViewAll)

            val i = Intent(this@MainActivity,ViewAll::class.java)

            viewAllGainer.setOnClickListener{
                i.putExtra("header","Top Gainer")
                i.putExtra("data", ArrayList(gainersAll))
                startActivity(i)
            }

            viewAllLoser.setOnClickListener{
                i.putExtra("header","Top Loser")
                i.putExtra("data", ArrayList(losersAll))
                startActivity(i)
            }

        }



    }

    fun fetchSuggestions(query: String, apiKey: String, callback: (List<SymbolSuggestion>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords=SAIC&apikey=$apiKey"
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: return@launch
                val json = JSONObject(body)
                val matches = json.optJSONArray("bestMatches") ?: return@launch

                val result = mutableListOf<SymbolSuggestion>()
                for (i in 0 until matches.length()) {
                    val obj = matches.getJSONObject(i)
                    val symbol = obj.optString("1. symbol", "")
                    val name = obj.optString("2. name", "")
                    result.add(SymbolSuggestion(symbol, name))
                }
                callback(result)
            } catch (e: Exception) {
                callback(emptyList())
            }
        }
    }


    suspend fun getTopGainers(apiKey: String): List<StockData> = withContext(Dispatchers.IO) {
        val url = "https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext emptyList<StockData>()

        val body = response.body?.string() ?: return@withContext emptyList<StockData>()
        val json = JSONObject(body)

        val gainersArray = json.optJSONArray("top_gainers") ?: return@withContext emptyList<StockData>()
        val gainersList = mutableListOf<StockData>()
        for (i in 0 until gainersArray.length()) {
            val item = gainersArray.getJSONObject(i)
            val symbol = item.optString("ticker", "")
            val price = item.optString("price", "")
            val change = item.optString("change_amount","")
            if (symbol.isNotBlank()) {
                gainersList.add(StockData(symbol, price,change))
            }
        }
        gainersList
    }

    suspend fun getTopLosers(apiKey: String): List<StockData> = withContext(Dispatchers.IO) {
        val url = "https://www.alphavantage.co/query?function=TOP_GAINERS_LOSERS&apikey=$apiKey"
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext emptyList<StockData>()

        val body = response.body?.string() ?: return@withContext emptyList<StockData>()
        val json = JSONObject(body)

        val losersArray = json.optJSONArray("top_losers") ?: return@withContext emptyList<StockData>()
        val losersList = mutableListOf<StockData>()
        for (i in 0 until losersArray.length()) {
            val item = losersArray.getJSONObject(i)
            val symbol = item.optString("ticker", "")
            val price = item.optString("price", "")
            val change = item.optString("change_amount", "")
            if (symbol.isNotBlank()) {
                losersList.add(StockData(symbol, price,change))
            }
        }
        losersList
    }
}
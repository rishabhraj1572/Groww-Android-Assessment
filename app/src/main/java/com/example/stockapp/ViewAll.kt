package com.example.stockapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ViewAll : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_all)


        val hedaer = intent.getStringExtra("header")
        val topText = findViewById<TextView>(R.id.topText)
        topText.setText(hedaer)

        val stockList = intent.getParcelableArrayListExtra<StockData>("data") ?: emptyList()

        val Adapter = StockAdapter(stockList) { stock ->
            Toast.makeText(this@ViewAll, "Item clicked: ${stock.symbol}", Toast.LENGTH_SHORT).show()
            val i= Intent(this@ViewAll,StockInformation::class.java)
            i.putExtra("symbol",stock.symbol)
            i.putExtra("price",stock.price)
            i.putExtra("change",stock.change)
            startActivity(i)
        }

        findViewById<RecyclerView>(R.id.rvAllGainers).apply {
            layoutManager = GridLayoutManager(this@ViewAll, 2)
            adapter = Adapter
        }

    }
}

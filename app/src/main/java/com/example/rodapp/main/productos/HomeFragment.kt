package com.example.rodapp.main.productos

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rodapp.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_productos)

        val listaProductos = listOf(
            Product("Producto 1", 25000.0, "Descripción 1", R.drawable.ic_launcher_foreground),
            Product("Producto 2", 30000.0, "Descripción 2", R.drawable.ic_launcher_foreground),
            Product("Producto 3", 15000.0, "Descripción 3", R.drawable.ic_launcher_foreground),
            Product("Producto 4", 45000.0, "Descripción 4", R.drawable.ic_launcher_foreground)
        )

        val adapter = ProductoAdapter(listaProductos)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return view
    }
}

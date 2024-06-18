package com.haruncanatali.yemektarifleri.adaptors

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.haruncanatali.yemektarifleri.databinding.RecyclerRowBinding
import com.haruncanatali.yemektarifleri.fragments.ListeFragmentDirections
import com.haruncanatali.yemektarifleri.models.Tarif

class TarifAdaptor (val tarifList : List<Tarif>) : RecyclerView.Adapter<TarifAdaptor.TarifHolder>() {
    class TarifHolder (val binding: RecyclerRowBinding) : RecyclerView.ViewHolder(binding.root){

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TarifHolder {
        val rcycRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TarifHolder(rcycRowBinding)
    }

    override fun getItemCount(): Int {
        return tarifList.size
    }

    override fun onBindViewHolder(holder: TarifHolder, position: Int) {
        holder.binding.isimRcycTxt.setText(tarifList.get(position).isim)

        holder.itemView.setOnClickListener{
            val action = ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "eski", id = tarifList.get(position).id)
            Navigation.findNavController(it).navigate(action)
        }
    }
}
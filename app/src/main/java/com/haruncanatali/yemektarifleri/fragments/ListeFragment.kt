package com.haruncanatali.yemektarifleri.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.room.Room
import com.haruncanatali.yemektarifleri.R
import com.haruncanatali.yemektarifleri.adaptors.TarifAdaptor
import com.haruncanatali.yemektarifleri.databinding.FragmentListeBinding
import com.haruncanatali.yemektarifleri.models.Tarif
import com.haruncanatali.yemektarifleri.roomDb.TarifDAO
import com.haruncanatali.yemektarifleri.roomDb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers

class ListeFragment : Fragment() {

    private var _binding : FragmentListeBinding? = null
    private val binding get() = _binding!!
    private lateinit var db : TarifDatabase
    private lateinit var tarifDao : TarifDAO
    private val mDisposable =  CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler").build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentListeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tarifEkleBtn.setOnClickListener {
            tarifEkleFunc(it)
        }

        binding.tariflerRcycView.layoutManager = LinearLayoutManager(requireContext(),)

        verileriAl()
    }

    private fun verileriAl(){
        mDisposable.add(
            tarifDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleListResponse)
        )
    }

    private fun handleListResponse(tarifler : List<Tarif>){
        val adaptor = TarifAdaptor(tarifler)
        binding.tariflerRcycView.adapter = adaptor
    }

    fun tarifEkleFunc(view:View) {
        val action =
            ListeFragmentDirections.actionListeFragmentToTarifFragment(bilgi = "yeni",id = 0)
        Navigation.findNavController(view).navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}
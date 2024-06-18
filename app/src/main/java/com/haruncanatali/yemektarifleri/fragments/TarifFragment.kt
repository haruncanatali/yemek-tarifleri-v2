package com.haruncanatali.yemektarifleri.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.Navigation
import androidx.room.Room
import com.google.android.material.snackbar.Snackbar
import com.haruncanatali.yemektarifleri.databinding.FragmentTarifBinding
import com.haruncanatali.yemektarifleri.models.Tarif
import com.haruncanatali.yemektarifleri.roomDb.TarifDAO
import com.haruncanatali.yemektarifleri.roomDb.TarifDatabase
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.ByteArrayOutputStream

class TarifFragment : Fragment() {

    private var _binding : FragmentTarifBinding? = null
    private val binding get() = _binding!!
    private lateinit var permissionLauncher: ActivityResultLauncher<String> // İzin istemek için
    private lateinit var activityResultLauncher: ActivityResultLauncher<Intent> // Galeriye gitmek için
    private var secilenGorsel : Uri?= null
    private var secilenBitmap : Bitmap? = null
    private lateinit var db : TarifDatabase
    private lateinit var tarifDao : TarifDAO
    private val mDisposable =  CompositeDisposable()
    private var secilenTarif : Tarif? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerLauncher()

        db = Room.databaseBuilder(requireContext(),TarifDatabase::class.java,"Tarifler")
            //.allowMainThreadQueries() -> db işlemi uzun sürebilir bu da ana thread de kullanıcı ui/ux kötü etkiler
            .build()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentTarifBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.kaydetBtn.setOnClickListener { tarifKaydetFunc(it) }
        binding.silBtn.setOnClickListener { tarifSiLFunc(it) }
        binding.yemekImg.setOnClickListener { gorselEkleFunc(it) }

        arguments?.let {
            val bilgi = TarifFragmentArgs.fromBundle(it).bilgi

            if(bilgi == "yeni"){
                binding.kaydetBtn.isEnabled = true
                binding.silBtn.isEnabled = false

                binding.isimText.setText("")
                binding.malzemeTxt.setText("")

                secilenTarif = null
            }
            else{
                binding.kaydetBtn.isEnabled = false
                binding.silBtn.isEnabled = true

                val id = TarifFragmentArgs.fromBundle(it).id
                mDisposable.add(
                    tarifDao.findById(id)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::handleGetByIdResponse)
                )
            }
        }
    }

    private fun handleGetByIdResponse(tarif: Tarif){
        binding.isimText.setText(tarif.isim)
        binding.malzemeTxt.setText(tarif.malzeme)
        binding.yemekImg.setImageBitmap(BitmapFactory.decodeByteArray(tarif.gorsel,0,tarif.gorsel.size))

        secilenTarif = tarif
    }

    fun tarifKaydetFunc(view: View){
        if(secilenBitmap == null){
            Toast.makeText(requireContext(),"Lütfen yemeğin görselini seçiniz.",Toast.LENGTH_LONG).show()
        }
        else{
            var byteArrayCevirici = ByteArrayOutputStream()
            var olusturulanBitmap = kucukBitmapOlustur(secilenBitmap!!, 300)
            olusturulanBitmap.compress(Bitmap.CompressFormat.PNG,50,byteArrayCevirici)

            val isim = binding.isimText.text.toString()
            val tarif = binding.malzemeTxt.text.toString()
            val gorsel = byteArrayCevirici.toByteArray()

            val tarifNesnesi = Tarif(isim,tarif,gorsel)

            mDisposable.add(tarifDao
                .insert(tarifNesnesi)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::handleInsert)
            )
        }

    }

    private fun handleInsert(){
        val action = TarifFragmentDirections.actionTarifFragmentToListeFragment()
        Navigation.findNavController(requireView()).navigate(action)
    }

    fun tarifSiLFunc(view: View){
        if(secilenTarif != null){
            mDisposable.add(
                tarifDao.delete(tarif = secilenTarif!!)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::handleInsert)
            )
        }
    }

    fun gorselEkleFunc(view: View){

        var permission = ""
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
            permission = Manifest.permission.READ_MEDIA_IMAGES
        }
        else{
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if(ContextCompat.checkSelfPermission(requireContext(),permission)
            != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),permission)){
                Snackbar.make(view,"Programın çalışması için galeriye erişim gerekiyor. Galeriye erişim izni veriyor musunuz?",Snackbar.LENGTH_INDEFINITE)
                    .setAction("İzin ver") {
                        permissionLauncher.launch(permission)
                    }.show()
            }
            else{
                permissionLauncher.launch(permission)
            }
        }
        else{ // Gerekli izin verilmiş
            val intentToGalery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            activityResultLauncher.launch(intentToGalery)
        }
    }

    private fun registerLauncher(){

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if(result.resultCode == AppCompatActivity.RESULT_OK) { // Görseli seçerse
                val intentFromResult = result.data
                if(intentFromResult != null){
                    try {
                        secilenGorsel = intentFromResult.data

                        if(Build.VERSION.SDK_INT >= 28){
                            secilenBitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(requireActivity().contentResolver, secilenGorsel!!))
                        }
                        else{
                            secilenBitmap = MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, secilenGorsel)
                        }

                        binding.yemekImg.setImageBitmap(secilenBitmap)
                    }
                    catch (e:Exception){
                        Toast.makeText(requireContext(),"Hata meydana geldi. -> ${e.localizedMessage}",Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){ // izin verildi
                val intentToGalery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGalery)
            }
            else{ // izin verilmedi
                Toast.makeText(requireContext(),"Galeri izinsiz bu programı kullanamazsınız.",Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun kucukBitmapOlustur(gelenBitmap:Bitmap, maxBoyut:Int) : Bitmap{
        val bitmapOrani : Double = gelenBitmap.width.toDouble() / gelenBitmap.height.toDouble()
        var width = 0
        var height = 0

        if(bitmapOrani > 1){ // görsel yatay
            width = maxBoyut
            height = (width.toDouble() / bitmapOrani).toInt()
        }
        else{ // görsel dikey
            height = maxBoyut
            width = (height.toDouble() * bitmapOrani).toInt()
        }

        return Bitmap.createScaledBitmap(gelenBitmap,width,height,true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        mDisposable.clear()
    }
}
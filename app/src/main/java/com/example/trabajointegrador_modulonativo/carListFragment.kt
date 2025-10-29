package com.example.trabajointegrador_modulonativo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.trabajointegrador_modulonativo.data.CarRepository
import com.example.trabajointegrador_modulonativo.data.ExpenseRepository
import com.example.trabajointegrador_modulonativo.data.SessionProvider
import com.example.trabajointegrador_modulonativo.databinding.FragmentCarListBinding
import com.example.trabajointegrador_modulonativo.model.Car
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModel
import com.example.trabajointegrador_modulonativo.viewmodel.CarViewModelFactory
import kotlinx.coroutines.launch

class carListFragment : Fragment() {

    private var _binding : FragmentCarListBinding? = null
    private val binding get() = _binding!!

    private val viewModel : CarViewModel by viewModels {
        CarViewModelFactory(CarRepository(), ExpenseRepository(), SessionProvider(), null)
    }

    private var carAdapter: SimpleItemRecyclerViewAdapter? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCarListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.getCars()

        binding.btnAgregarVehiculo.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_form)

        }

        binding.btnVerGastos?.setOnClickListener {
            findNavController().navigate(R.id.action_list_to_expenses)
        }
    }

    private fun setupRecyclerView() {
        carAdapter = SimpleItemRecyclerViewAdapter(
            emptyList(),
            binding.root.findViewById(R.id.car_detail_nav_container)
        )
        binding.carList.adapter = carAdapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                        binding.carList.isVisible = !isLoading
                    }
                }

                launch {
                    viewModel.carsState.collect { cars ->
                        carAdapter?.updateData(cars)
                    }
                }

                launch {
                    viewModel.error.collect { errorMsg ->
                        if(errorMsg != null) {
                            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }



    class SimpleItemRecyclerViewAdapter(
        private var values: List<Car>,
        private val itemDetailFragmentContainer: View?
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: com.example.trabajointegrador_modulonativo.databinding.ItemCarBinding) : RecyclerView.ViewHolder(binding.root)

        @SuppressLint("NotifyDataSetChanged")
        fun updateData(newValues: List<Car>) {
            values = newValues
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = com.example.trabajointegrador_modulonativo.databinding.ItemCarBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.binding.tvTitle.text = "${item.brand} ${item.model}"
            holder.binding.tvSubtitle.text = "Patente: ${item.licensePlate} - Última actualización: ${item.lastUpdate}"

            val requestOptions = RequestOptions()
                .transform(CenterCrop(), RoundedCorners(24))

            if(item.imageUrl != null) {
                Glide.with(holder.itemView)
                    .load(item.imageUrl)
                    .apply(requestOptions)
                    .placeholder(R.drawable.generic_car_icon)
                    .error(R.drawable.generic_car_icon)
                    .into(holder.binding.imgThumb)
            } else {
                Glide.with(holder.itemView)
                    .load(R.drawable.generic_car_icon)
                    .apply(requestOptions)
                    .into(holder.binding.imgThumb)
            }


            holder.itemView.tag = item
            holder.itemView.setOnClickListener { v ->

                val bundle = Bundle().apply { putString(carDetailFragment.ARG_ITEM_ID, item.id) }
                if (itemDetailFragmentContainer != null) {
                    itemDetailFragmentContainer.findNavController().navigate(R.id.fragment_car_detail, bundle)
                } else {
                    v.findNavController().navigate(R.id.show_car_detail, bundle)
                }
            }
        }


        override fun getItemCount() = values.size



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



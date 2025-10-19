package com.example.trabajointegrador_modulonativo

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.trabajointegrador_modulonativo.placeholder.PlaceholderContent;
import com.example.trabajointegrador_modulonativo.databinding.FragmentCarListBinding
import com.example.trabajointegrador_modulonativo.databinding.CarListContentBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
/**
 * A Fragment representing a list of Pings. This fragment
 * has different presentations for handset and larger screen devices. On
 * handsets, the fragment presents a list of items, which when touched,
 * lead to a {@link carDetailFragment} representing
 * item details. On larger screens, the Navigation controller presents the list of items and
 * item details side-by-side using two vertical panes.
 */

class carListFragment : Fragment() {

    /**
     * Method to intercept global key events in the
     * item list fragment to trigger keyboard shortcuts
     * Currently provides a toast when Ctrl + Z and Ctrl + F
     * are triggered
     */
    private val unhandledKeyEventListenerCompat =
        ViewCompat.OnUnhandledKeyEventListenerCompat { v, event ->
            if (event.keyCode == KeyEvent.KEYCODE_Z && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Undo (Ctrl + Z) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else if (event.keyCode == KeyEvent.KEYCODE_F && event.isCtrlPressed) {
                Toast.makeText(
                    v.context,
                    "Find (Ctrl + F) shortcut triggered",
                    Toast.LENGTH_LONG
                ).show()
                true
            }
            false
        }

    private var _binding: FragmentCarListBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentCarListBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.addOnUnhandledKeyEventListener(view, unhandledKeyEventListenerCompat)

        val recyclerView: RecyclerView = binding.carList

        // Leaving this not using view binding as it relies on if the view is visible the current
        // layout configuration (layout, layout-sw600dp)
        val itemDetailFragmentContainer: View? = view.findViewById(R.id.car_detail_nav_container)

        setupRecyclerView(recyclerView, itemDetailFragmentContainer)
    }

    private fun setupRecyclerView(
        recyclerView: RecyclerView,
        itemDetailFragmentContainer: View?
    ) {

        recyclerView.adapter = SimpleItemRecyclerViewAdapter(
            PlaceholderContent.ITEMS, itemDetailFragmentContainer
        )
    }

    class SimpleItemRecyclerViewAdapter(
        private val values: List<PlaceholderContent.PlaceholderItem>,
        private val itemDetailFragmentContainer: View?
    ) :
        RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car, parent, false)
            return ViewHolder(view)

        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.tvTitle.text = item.content
            holder.tvSubtitle.text = "Última actualización: ${item.lastUpdate}"

            Glide.with(holder.itemView)
                .load(item.imageUrl)
                .transform(RoundedCorners(16)) // opcional, redondea la imagen
                .into(holder.imgThumb)

            holder.itemView.tag = item
            holder.itemView.setOnClickListener { v ->
                // mantené la misma lógica de navegación que ya tenés:
                val bundle = Bundle().apply { putString(carDetailFragment.ARG_ITEM_ID, item.id) }
                if (itemDetailFragmentContainer != null) {
                    itemDetailFragmentContainer.findNavController().navigate(R.id.fragment_car_detail, bundle)
                } else {
                    v.findNavController().navigate(R.id.show_car_detail, bundle)
                }
            }
        }


        override fun getItemCount() = values.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
            val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
            val tvSubtitle: TextView = itemView.findViewById(R.id.tvSubtitle)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
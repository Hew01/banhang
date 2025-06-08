package com.example.banhangs.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterInside
import com.bumptech.glide.request.RequestOptions
import com.example.banhangs.Model.SliderModel
import com.example.banhangs.databinding.SliderItemContainerBinding


//tác dụng là tạo 1 thanh trượt hình ảnh

class SliderAdapter(private var sliderItems:List<SliderModel>, private  var viewPager2: ViewPager2) :
    RecyclerView.Adapter<SliderAdapter.SliderViewholder>() {

        private lateinit var context: Context

        //có tác dụng gán lại Item:1 và làm mới adapter:2
        private var runnable = Runnable {
            sliderItems = sliderItems
       //     notifyDataSetChanged() 
        }


    //đại diện cho 1 ảnh trong thanh trượt
    class SliderViewholder(private var binding: SliderItemContainerBinding): RecyclerView.ViewHolder(binding.root) {
                   fun setImage(sliderItems: SliderModel, context: Context){
                      Glide.with(context)
                          .load(sliderItems.url) //tải ảnh
                          .apply { RequestOptions().transform(CenterInside()) } //căn chỉnh tỉ lệ ảnh
                          .into(binding.imageSlide) //hiển thị ảnh
                   }
    }

    //Tạo một SliderViewholder mới khi cần.
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SliderAdapter.SliderViewholder {
       context = parent.context
        val binding = SliderItemContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false) //chuyển xml sang activity để có giao diện
        return SliderViewholder(binding)
    }

    //Liên kết dữ liệu với SliderViewholder tại vị trí cụ thể kiểu như ấn vào ảnh nào là nó biết vị trí ảnh đó
    override fun onBindViewHolder(holder: SliderAdapter.SliderViewholder, position: Int) {
      holder.setImage(sliderItems[position],context)
        if(position == sliderItems.lastIndex -1){ //nghĩa là nếu nó là ảnh thứ 2 trở đi hén sẽ ẩn trong viewpager2, tra yt viewPager2 để dễ hình dung
            viewPager2.post(runnable)
        }
    }

    override fun getItemCount():  Int=sliderItems.size
}

//Cách hoạt động
//Khởi tạo: Adapter được tạo với danh sách SliderModel và một instance của ViewPager2.
//Tạo View: onCreateViewHolder nạp giao diện cho từng mục trượt và tạo SliderViewholder.
//Liên kết dữ liệu: onBindViewHolder tải hình ảnh cho từng mục bằng Glide và kiểm tra nếu gần cuối danh sách để chạy runnable.
//Cuộn vòng: runnable dường như được thiết kế để reset hoặc làm mới thanh trượt (ví dụ: cuộn vô hạn), nhưng hiện tại chưa thay đổi dữ liệu.




//test
//class SliderAdapter(private var sliderItems: List<SliderModel>, private var viewPager2: ViewPager2) :
//    RecyclerView.Adapter<SliderAdapter.SliderViewholder>() {
//
//    private lateinit var context: Context
//
//    private var runnable = Runnable {
//        sliderItems = sliderItems
//        notifyDataSetChanged()
//    }
//
//    // SliderViewholder đại diện cho 1 ảnh trong thanh trượt
//    class SliderViewholder(private var binding: SliderItemContainerBinding) : RecyclerView.ViewHolder(binding.root) {
//        fun setImage(sliderItems: SliderModel, context: Context) {
//            // Sử dụng Glide để tải ảnh cục bộ từ drawable
//            Glide.with(context)
//                .load(getDrawableResource(sliderItems.imageName, context)) // Chọn ảnh từ drawable
//                .apply(RequestOptions().transform(CenterInside())) // Căn chỉnh tỉ lệ ảnh
//                .into(binding.imageSlide) // Hiển thị ảnh
//        }
//
//        // Hàm để lấy tài nguyên drawable từ tên ảnh
//        private fun getDrawableResource(imageName: String, context: Context): Int {
//            return context.resources.getIdentifier(imageName, "drawable", context.packageName)
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewholder {
//        context = parent.context
//        val binding = SliderItemContainerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return SliderViewholder(binding)
//    }
//
//    override fun onBindViewHolder(holder: SliderViewholder, position: Int) {
//        holder.setImage(sliderItems[position], context)
//        if (position == sliderItems.lastIndex - 1) {
//            viewPager2.post(runnable)
//        }
//    }
//
//    override fun getItemCount(): Int = sliderItems.size
//}

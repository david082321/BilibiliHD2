package com.duzhaokun123.bilibilihd2.ui.main

import android.annotation.SuppressLint
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.duzhaokun123.bilibilihd2.R
import com.duzhaokun123.bilibilihd2.bases.BaseSRRVFragment
import com.duzhaokun123.bilibilihd2.model.DynamicCardModel
import com.duzhaokun123.bilibilihd2.utils.*
import com.duzhaokun123.generated.Settings
import com.scwang.smart.refresh.layout.api.RefreshLayout
import io.material.catalog.tableofcontents.GridDividerDecoration

class DynamicFragment : BaseSRRVFragment() {
    class DynamicModel : ViewModel() {
        val dynamicModel = MutableLiveData<List<DynamicCardModel>>(emptyList())
        val offsetDynamicId = MutableLiveData<Long>(0)
        val page = MutableLiveData(1)
    }

    private val model by activityViewModels<DynamicModel>()
    private val dynamicCardWidth by lazy { Settings.dynamicCardWidthDp.dpToPx() }

    override fun onRefresh(refreshLayout: RefreshLayout) {
        runIOCatchingResultRunMain(context, { bilibiliClient.vcAPI.dynamicNew().await() },
            { srl.finishRefresh(false) })
        { dynamicNew ->
            model.dynamicModel.value = DynamicCardModel.parse(dynamicNew)
            model.offsetDynamicId.value = dynamicNew.data.historyOffset
            model.page.value = 1
            srl.finishRefresh()
            adapter = adapter
        }
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
        runIOCatchingResultRunMain(
            context, {
                bilibiliClient.vcAPI.dynamicHistory(
                    model.offsetDynamicId.value!!, model.page.value!! + 1
                ).await()
            }, { srl.finishLoadMore(false) }) { dynamicHistory ->
            val a = DynamicCardModel.parse(dynamicHistory)
            model.offsetDynamicId.value = dynamicHistory.data.nextOffset
            model.page.value = model.page.value!! + 1
            val oldCount = model.dynamicModel.value!!.size
            model.dynamicModel.value = model.dynamicModel.value!! + a
            srl.finishLoadMore()
            adapter!!.notifyItemRangeInserted(oldCount, a.size)
        }
    }

    override fun initLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(context, 1)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun initViews() {
        super.initViews()
        runCatching { baseBinding.rv.removeItemDecorationAt(0) }
        baseBinding.rv.addItemDecoration(GridDividerDecoration(1.dpToPx(), ColorUtils.setAlphaComponent(requireContext().theme.getAttr(R.attr.colorOnSurface).data, (255 * 0.12).toInt()), 1), 0)
        baseBinding.rv.addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
            var a = (v.width - dynamicCardWidth) / 2
            if (a < 0) a = 0
            if (a * 2 <= dynamicCardWidth / 3) a = 0
            v.updatePadding(left = a, right = a)
        }
        baseBinding.root.setOnTouchListener { _, motionEvent -> baseBinding.srl.onTouchEvent(motionEvent) }
    }

    override fun initData() {
        adapter = DynamicAdapter(this)
        if (model.dynamicModel.value!!.isEmpty()) srl.autoRefresh()
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        with(insets.maxSystemBarsDisplayCutout) {
            baseBinding.srl.updatePadding(left = left, right = right, bottom = bottom)
            baseBinding.cf.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bottom
            }
        }
    }
}
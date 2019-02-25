package com.example.ccg.pulltorefresh

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.support.annotation.NonNull
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.*
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView

/**
 * description ：弹性拉伸刷新布局
 * author : chenchenggui
 * creation date: 2018/10/19
 */
class PullToRefreshLayout(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attr: AttributeSet?) : this(context, attr, 0)

    companion object {
        private const val BACK_ANIM_DUR = 500L
        private const val SCROLL_ANIM_DUR = 600L
        private const val BEZIER_ANIM_DUR = 350L
        private const val ROTATION_ANIM_DUR = 200L
        private const val DEFAULT_MARGIN_RIGHT = 10f
        private const val DEFAULT_PULL_WIDTH = 100f
        private const val DEFAULT_MOVE_MAX_DIMEN = 50f
        private const val DEFAULT_MORE_VIEW_TEXT_SIZE = 15f
        private const val DEFAULT_FOOTER_WIDTH = 50f
        private const val DEFAULT_FOOTER_PULL_MAX_TOP = 30f
        private const val DEFAULT_FOOTER_BG_RADIUS = 20f
        private const val DEFAULT_FOOTER_VERTICAL_MARGIN = 10f
        private const val DEFAULT_VISIBLE_WIDTH = 40f

        private const val DEFAULT_SCAN_MORE = "查看更多"
        private const val DEFAULT_RELEASE_SCAN_MORE = "释放查看"
        private const val ROTATION_ANGLE = 180f

        private val animationInterpolator = LinearInterpolator()

        /**
         * 滑动最大距离
         */
        private var moreViewMoveMaxDimen = 0f
        private var moreViewTextColor = Color.BLACK
        private var moreViewTextSize = 0f
        private var scanMore: String? = null
        private var releaseScanMore: String? = null
        /**
         * 默认"查看更多"可见值
         */
        private var defaultOffsetX = 0f
    }

    private var touchStartX = 0f
    private var touchCurrX = 0f
    private var touchLastX = 0f

    /**
     * 拉伸距离
     */
    private var pullWidth = 0f
    /**
     * more_view右边距
     */
    private var moreViewMarginRight = 0

    /**
     * 脚布局垂直方向边距
     */
    private var footerVerticalMargin = 0
    /**
     * 脚布局宽度
     */
    private var footerWidth = 0f
    /**
     * 脚布局拉动时最大缩放高度
     */
    private var footerPullMaxTop = 0f
    /**
     * 脚局部背景颜色
     */
    private var footerViewBgColor: Int = Color.GRAY
    /**
     * 脚布局矩形圆角
     */
    private var footerViewBgRadius: Float = 0f
    private var animStartTop = 0f

    private var isRefresh = false
    private var scrollState = false

    private var childView: View? = null
    private var footerView: AnimView? = null
    private var moreView: View? = null
    /**
     * 加载更多文字
     */
    private var moreText: TextView? = null
    /**
     * 可显示拖拽方向图标
     */
    private var arrowIv: ImageView? = null

    private var backAnimator: ValueAnimator? = null
    private var arrowRotateAnim: RotateAnimation? = null
    private var arrowRotateBackAnim: RotateAnimation? = null
    private var mOffsetAnimator: ValueAnimator? = null
    private var isFooterViewShow = false
    /**
     * 嵌套滑动后是否需要继续滚动
     */
    private var isNeedScroll = false

    /**
     * 滑动监听
     */
    private var scrollListener: ((Boolean) -> Unit)? = null

    fun setOnScrollListener(listener: (Boolean) -> Unit) {
        this.scrollListener = listener
    }

    /**
     * 刷新监听
     */
    private var refreshListener: (() -> Unit)? = null

    fun setOnRefreshListener(listener: (() -> Unit)) {
        this.refreshListener = listener
    }

    private var interpolator = DecelerateInterpolator(10f)

    init {
        val displayMetrics = resources.displayMetrics
        val defaultPullWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_PULL_WIDTH, displayMetrics)
        val defaultMoreViewMoveMaxDimen = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_MOVE_MAX_DIMEN, displayMetrics)
        val defaultMoreViewTextSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_MORE_VIEW_TEXT_SIZE, displayMetrics)
        val defaultFooterWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_FOOTER_WIDTH, displayMetrics)
        val defaultFooterTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_FOOTER_PULL_MAX_TOP, displayMetrics)
        val defaultFooterRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_FOOTER_BG_RADIUS, displayMetrics)
        val defaultFooterVerticalMargin = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_FOOTER_VERTICAL_MARGIN, displayMetrics)
        val defaultMoreViewMarginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_MARGIN_RIGHT, displayMetrics)
        val defaultVisibleWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                DEFAULT_VISIBLE_WIDTH, displayMetrics)

        scanMore = DEFAULT_SCAN_MORE
        releaseScanMore = DEFAULT_RELEASE_SCAN_MORE

        val ta = context.obtainStyledAttributes(attrs, R.styleable.PullToRefreshLayout)
        pullWidth = ta.getDimension(R.styleable.PullToRefreshLayout_pullWidth, defaultPullWidth)
        moreViewMoveMaxDimen = ta.getDimension(R.styleable.PullToRefreshLayout_moreViewMoveMaxDimen,
                defaultMoreViewMoveMaxDimen)
        moreViewTextColor = ta.getColor(R.styleable.PullToRefreshLayout_moreViewTextColor, Color.BLACK)
        moreViewTextSize = ta.getDimension(R.styleable.PullToRefreshLayout_moreViewTestSize, defaultMoreViewTextSize)
        defaultOffsetX = ta.getDimension(R.styleable.PullToRefreshLayout_moreViewVisibleWidth,
                defaultVisibleWidth)
        moreViewMarginRight = -ta.getDimension(R.styleable.PullToRefreshLayout_moreViewMarginRight,
                defaultMoreViewMarginRight).toInt()

        footerViewBgColor = ta.getColor(R.styleable.PullToRefreshLayout_footerBgColor, Color.GRAY)
        footerWidth = ta.getDimension(R.styleable.PullToRefreshLayout_footerWidth, defaultFooterWidth)
        footerPullMaxTop = ta.getDimension(R.styleable.PullToRefreshLayout_footerPullMaxTop,
                defaultFooterTop)
        footerViewBgRadius = ta.getDimension(R.styleable.PullToRefreshLayout_footerBgRadius, defaultFooterRadius)
        footerVerticalMargin = ta.getDimension(R.styleable.PullToRefreshLayout_footerVerticalMargin,
                defaultFooterVerticalMargin).toInt()

        if (ta.hasValue(R.styleable.PullToRefreshLayout_scanMoreText)) {
            scanMore = ta.getString(R.styleable.PullToRefreshLayout_scanMoreText)
        }
        if (ta.hasValue(R.styleable.PullToRefreshLayout_releaseScanMoreText)) {
            releaseScanMore = ta.getString(R.styleable.PullToRefreshLayout_releaseScanMoreText)
        }
        ta.recycle()

        post {
            childView = getChildAt(0)
            if (childView is RecyclerView) {
                val recyclerView = childView as RecyclerView
                recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        super.onScrollStateChanged(recyclerView, newState)
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            //滑动停止后，最后一条item显示时滑动到底部，展示查看更多
                            if (!canScrollRight()) {
                                isFooterViewShow = true
                                childView?.translationX = -defaultOffsetX
                                footerView?.layoutParams?.width = defaultOffsetX.toInt()
                                footerView?.requestLayout()

                                moveMoreView(defaultOffsetX, false)
                                animateScroll(0f, SCROLL_ANIM_DUR.toInt(), false)

                                isNeedScroll = false
                            }
                        }
                    }
                })
            }

            addFooterView()
            addMoreView()
            initBackAnim()
            initRotateAnim()
        }
    }

    fun reset() {
        removeView(footerView)
        removeView(moreView)
        childView?.translationX = 0f
        if (scrollX!=0){
            scrollX=0
        }
        addFooterView()
        addMoreView()
        initBackAnim()
        initRotateAnim()
    }

    private fun addFooterView() {
        val params = FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
        params.topMargin = footerVerticalMargin
        params.bottomMargin = footerVerticalMargin
        params.gravity = Gravity.RIGHT

        footerView = AnimView(context).apply {
            layoutParams = params
            setBgColor(footerViewBgColor)
            setBgRadius(footerViewBgRadius)
            pullWidth = footerWidth.toInt()
//            pullDelta=this@PullToRefreshLayout.pullWidth.toInt()

            bezierBackDur = BEZIER_ANIM_DUR
        }
        addViewInternal(footerView!!)
    }

    private fun addMoreView() {
        val params = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
        params.setMargins(0, 0, moreViewMarginRight, 0)

        moreView = LayoutInflater.from(context).inflate(R.layout.item_load_more, this, false).apply {
            layoutParams = params
            moreText = findViewById(R.id.tv_more_text)
            moreText?.setTextSize(TypedValue.COMPLEX_UNIT_PX, moreViewTextSize)
            moreText?.setTextColor(moreViewTextColor)
        }
        addViewInternal(moreView!!)
    }

    private fun initBackAnim() {
        if (childView == null) {
            return
        }
        backAnimator = ValueAnimator.ofFloat(pullWidth, 0f).apply {
            addListener(AnimListener())

            addUpdateListener {
                val value: Float = it.animatedValue as Float
                var offsetX = value
                var offsetY = interpolator.getInterpolation(value / height) * value

                if (value <= footerWidth) {
                    offsetX *= interpolator.getInterpolation(value / footerWidth)
                    if (offsetX <= defaultOffsetX) {
                        offsetX = defaultOffsetX
                    }

                    footerView?.layoutParams?.width = offsetX.toInt()
                    footerView?.top = offsetY
                    footerView?.requestLayout()
                } else {
                    //记录当前收缩动画的宽高
                    if (offsetY >= animStartTop) {
                        offsetY = animStartTop
                    }
                    footerView?.top = offsetY
                    footerView?.layoutParams?.width = offsetX.toInt()
                }

                childView?.translationX = -offsetX
                moveMoreView(offsetX, true)
            }

            //动画时长
            duration = BACK_ANIM_DUR
        }
    }

    private fun moveMoreView(offsetX: Float, release: Boolean, move: Boolean = false) {
        val dx = offsetX / 2
        moreView?.visibility = if (move) View.VISIBLE else View.INVISIBLE
        if (dx <= moreViewMoveMaxDimen) {
            moreView?.translationX = -dx
            if (!release && switchMoreText(scanMore)) {
                arrowIv?.clearAnimation()
                arrowIv?.startAnimation(arrowRotateBackAnim)
            }
        } else {
            if (switchMoreText(releaseScanMore)) {
                arrowIv?.clearAnimation()
                arrowIv?.startAnimation(arrowRotateAnim)
            }
        }
    }

    private fun switchMoreText(text: String?): Boolean {
        if (TextUtils.equals(text, moreText?.text.toString())) {
            return false
        }
        moreText?.text = text
        return true
    }

    private fun initRotateAnim() {
        val pivotType = Animation.RELATIVE_TO_SELF
        val pivotValue = 0.5f
        arrowRotateAnim = RotateAnimation(0f, ROTATION_ANGLE, pivotType, pivotValue, pivotType,
                pivotValue).apply {
            interpolator = animationInterpolator
            duration = ROTATION_ANIM_DUR
            fillAfter = true
        }
        arrowRotateBackAnim = RotateAnimation(ROTATION_ANGLE, 0f, pivotType, pivotValue, pivotType,
                pivotValue).apply {
            interpolator = animationInterpolator
            duration = ROTATION_ANIM_DUR
            fillAfter = true
        }
    }

    private fun addViewInternal(@NonNull child: View) {
        super.addView(child)
    }

    override fun addView(child: View?) {
        if (childCount >= 1) {
            throw  RuntimeException("only can attach one child")
        }
        childView = child as RecyclerView?
        super.addView(child)
    }

    inner class AnimListener : Animator.AnimatorListener {
        override fun onAnimationRepeat(animation: Animator?) {
        }

        override fun onAnimationEnd(animation: Animator?) {
            moreText?.text = scanMore
            arrowIv?.clearAnimation()
            isRefresh = false
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
            if (isRefresh) {
                refreshListener?.invoke()
            }
            animStartTop = footerView?.top!!
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (isRefresh) return true

        when (ev?.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = ev.x
                touchLastX = ev.x
                touchCurrX = touchStartX
                setScrollState(false)
            }
            MotionEvent.ACTION_MOVE -> {
                val currX = ev.x
                val dx = touchStartX - currX
                touchLastX = currX

                //拦截条件
                if (dx > 10 && !canScrollRight() && scrollX >= 0) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    setScrollState(true)
                    return true
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (isRefresh) {
            return super.onTouchEvent(event)
        }
        when (event?.action) {
            MotionEvent.ACTION_MOVE -> {
                touchCurrX = event.x
                var dx = touchStartX - touchCurrX

                if (childView == null) return true

                dx = Math.min(pullWidth * 2, dx)
                dx = Math.max(0f, dx)
                val unit = dx / 2

                //计算偏移量
                var offsetX = interpolator.getInterpolation(unit / pullWidth) * unit
                var offsetY = interpolator.getInterpolation(unit / height) * unit -
                        moreViewMoveMaxDimen
                if (offsetY >= footerPullMaxTop) {
                    offsetY = footerPullMaxTop
                }

                //偏移量加上默认脚布局宽度
                if (isFooterViewShow) {
                    offsetX += defaultOffsetX
                }
                var tranX = offsetX
                //位移最大值。超过的部分缩短为滑动距离的一半
                val max = (pullWidth * 0.8f + defaultOffsetX)
                if (tranX >= max) {
                    tranX = (tranX - max) * 0.5f + max
                }

                childView?.translationX = -tranX
                footerView?.layoutParams?.width = offsetX.toInt()
                footerView?.top = offsetY
                footerView?.requestLayout()

                moveMoreView(offsetX, false, true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (childView == null || childView?.translationX!! >= 0) {
                    return true
                }
                val childDx = Math.abs(childView?.translationX!!)

                if (reachReleasePoint()) {
                    isFooterViewShow = true
                    isRefresh = true
                }

                backAnimator?.setFloatValues(childDx, 0f)
                backAnimator?.start()

                if (childDx >= footerWidth) {
                    footerView?.releaseDrag()

//                    if (reachReleasePoint()) {
//                        isRefresh = true
//                    }
                }
                setScrollState(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun reachReleasePoint(): Boolean = TextUtils.equals(releaseScanMore, moreText?.text
            .toString())

    private fun canScrollRight(): Boolean = childView?.canScrollHorizontally(1) ?: false

    private fun setScrollState(scrollState: Boolean) {
        if (this.scrollState == scrollState) return
        this.scrollState = scrollState
        scrollListener?.invoke(scrollState)
    }

    override fun onStartNestedScroll(child: View?, target: View?, nestedScrollAxes: Int): Boolean {
        return nestedScrollAxes and ViewCompat.SCROLL_AXIS_HORIZONTAL != 0
    }

    override fun onNestedPreScroll(target: View?, dx: Int, dy: Int, consumed: IntArray) {
        val hiddenMoreView = dx < 0 && scrollX > -defaultOffsetX.toInt() && !canScrollRight()
                && footerView?.width != 0
        val showMoreView = dx > 0 && scrollX < 0 && !canScrollRight()
        if (hiddenMoreView || showMoreView) {
            isNeedScroll = true
            scrollBy(dx, 0)
            consumed[0] = dx
        }
    }

    /**
     * 停止嵌套滑动后修正处理
     */
    override fun onStopNestedScroll(child: View?) {
        super.onStopNestedScroll(child)
        if (isNeedScroll) {
            animateScroll(0f, SCROLL_ANIM_DUR.toInt(), false)
        }
    }

    override fun onNestedFling(target: View?, velocityX: Float, velocityY: Float, consumed: Boolean): Boolean {
        var realConsumed = consumed

        if (target is RecyclerView && velocityX > 0) {
            val firstChild = target.getChildAt(0)
            val childAdapterPosition = target.getChildAdapterPosition(firstChild)
            realConsumed = childAdapterPosition > 3
        }

        if (!realConsumed) {
            animateScroll(velocityX, computeDuration(0f), realConsumed)
        } else {
            animateScroll(velocityX, computeDuration(velocityX), realConsumed)
        }
        return true
    }

    private fun computeDuration(velocityX: Float): Int {
        val distance: Int = if (velocityX > 0) {
            Math.abs(defaultOffsetX.toInt() - scrollX)
        } else {
            Math.abs(defaultOffsetX.toInt() - (defaultOffsetX.toInt() - scrollX))
        }

        val duration: Int
        val realVelocityX = Math.abs(velocityX)

        duration = if (realVelocityX > 0) {
            3 * Math.round(1000 * (distance / realVelocityX))
        } else {
            val distanceRatio = distance.toFloat() / width
            ((distanceRatio + 1) * 150).toInt()
        }

        return duration
    }

    override fun onNestedPreFling(target: View?, velocityX: Float, velocityY: Float): Boolean {
        //隐藏moreView过程中消费掉fling
        var isAnimRunning = false
        if (mOffsetAnimator != null) {
            isAnimRunning = mOffsetAnimator!!.isRunning
        }
        if (velocityX < 0 && scrollX >= -defaultOffsetX.toInt() && !canScrollRight()
                || isAnimRunning) {
            return true
        }
        return false
    }

    private fun animateScroll(velocityX: Float, duration: Int, consumed: Boolean) {
        if (canScrollRight()) {
            return
        }
        val currentOffset = scrollX
        if (mOffsetAnimator == null) {
            mOffsetAnimator = ValueAnimator()
            mOffsetAnimator?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(p0: Animator?) {
                }

                override fun onAnimationStart(p0: Animator?) {
                }

                override fun onAnimationEnd(p0: Animator?) {
                    isNeedScroll = false
                }

                override fun onAnimationCancel(p0: Animator?) {
//                    isNeedScroll=true
                }

            })
            mOffsetAnimator?.addUpdateListener { animation ->
                if (animation.animatedValue is Int) {
                    scrollTo(animation.animatedValue as Int, 0)
                }
            }
        } else {
            mOffsetAnimator?.cancel()
        }
        mOffsetAnimator?.duration = Math.min(duration, SCROLL_ANIM_DUR.toInt()).toLong()

        if (velocityX >= 0) {
            mOffsetAnimator?.setIntValues(currentOffset, 0)
            mOffsetAnimator?.start()
        } else {
            //如果子View没有消耗down事件 那么就让自身滑倒0位置
            if (!consumed) {
                mOffsetAnimator?.setIntValues(currentOffset, 0)
                mOffsetAnimator?.start()
            }

        }
    }

    /**
     * 限定滚动的范围，scrollBy默认调用scrollTo
     */
    override fun scrollTo(x: Int, y: Int) {
        var realX = x
        if (x >= 0) {
            realX = 0
        }
        if (x <= -defaultOffsetX) {
            realX = -defaultOffsetX.toInt()
        }
        if (realX != scrollX) {
            super.scrollTo(realX, y)
        }
    }

    /**
     * 获取嵌套滑动的轴
     * @see ViewCompat.SCROLL_AXIS_HORIZONTAL 水平
     * @see ViewCompat.SCROLL_AXIS_VERTICAL 垂直
     * @see ViewCompat.SCROLL_AXIS_NONE 都支持
     */
    override fun getNestedScrollAxes(): Int {
        return ViewCompat.SCROLL_AXIS_HORIZONTAL
    }
}


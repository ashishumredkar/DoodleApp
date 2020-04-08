package sample.app.doodleappassignment

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.*



/**
 * Created by Ashish on 4/6/2020.
 */

class PaletteView : View {

    private var mPaint: Paint? = null
    private var mPath: Path? = null
    private var mLastX: Float = 0.toFloat()
    private var mLastY: Float = 0.toFloat()
    private var mBufferBitmap: Bitmap? = null
    private var mBufferCanvas: Canvas? = null

    private var mDrawingList: MutableList<DrawingInfo>? = null
    private var mRemovedList: MutableList<DrawingInfo>? = null

    private var mXferModeClear: Xfermode? = null
    private var mXferModeDraw: Xfermode? = null
    var penSize: Int = 0
        private set
    var eraserSize: Int = 0
    var penAlpha = 255
        set(alpha) {
            field = alpha
            if (mode == Mode.DRAW) {
                mPaint!!.alpha = alpha
            }
        }

    private var mCanEraser: Boolean = false

    private var mCallback: Callback? = null

    internal var lastAction: Int = 0
    private var lastUsedImage: String? = null

    var mode = Mode.DRAW
        set(mode) {
            if (mode != this.mode) {
                field = mode
                if (this.mode == Mode.DRAW) {
                    mPaint!!.xfermode = mXferModeDraw
                    mPaint!!.strokeWidth = penSize.toFloat()
                } else {
                    mPaint!!.xfermode = mXferModeClear
                    mPaint!!.strokeWidth = eraserSize.toFloat()
                }
            }
        }

    var penColor: Int
        get() = mPaint!!.color
        set(color) {
            mPaint!!.color = color
        }

    enum class Mode {
        DRAW,
        ERASER
    }


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    interface Callback {
        fun onUndoRedoStatusChanged()
    }

    fun setCallback(callback: Callback) {
        mCallback = callback
    }

    private fun init() {
        isDrawingCacheEnabled = true
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        mPaint!!.style = Paint.Style.STROKE
        mPaint!!.isFilterBitmap = true
        mPaint!!.strokeJoin = Paint.Join.ROUND
        mPaint!!.strokeCap = Paint.Cap.ROUND
        penSize = DimenUtils.dp2pxInt(3f)
        eraserSize = DimenUtils.dp2pxInt(30f)
        mPaint!!.strokeWidth = penSize.toFloat()
        mPaint!!.color = -0x1000000
        mXferModeDraw = PorterDuffXfermode(PorterDuff.Mode.SRC)
        mXferModeClear = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        mPaint!!.xfermode = mXferModeDraw
    }

    fun initBuffer() {
        mBufferBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        mBufferCanvas = Canvas(mBufferBitmap!!)
    }

    fun setBack(path: String) {
        this.lastUsedImage = path
        mBufferBitmap = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val options = BitmapFactory.Options()
        options.inMutable = true
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        var bitmap = BitmapFactory.decodeFile(path, options)

        bitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        mBufferBitmap = bitmap
        mBufferCanvas = Canvas(mBufferBitmap!!)
    }

    private abstract class DrawingInfo {
        internal var paint: Paint? = null
        internal abstract fun draw(canvas: Canvas?)
    }

//    private class PathDrawingInfo : DrawingInfo() {
//        internal var path: Path? = null
//        override fun draw(canvas: Canvas?) {
//            canvas.drawPath(path!!, paint!!)
//        }
//    }
    private  class PathDrawingInfo:DrawingInfo(){

    internal var path: Path? = null
    override fun draw(canvas: Canvas?) {
          canvas?.drawPath(path!!, paint!!)
            //drawPath(path!!, paint!!)
        }

    }

    fun setPenRawSize(size: Int) {
        penSize = size
        if (mode == Mode.DRAW) {
            mPaint!!.strokeWidth = penSize.toFloat()
        }
    }

    private fun reDraw() {
        if (mDrawingList != null) {
            mBufferBitmap!!.eraseColor(Color.TRANSPARENT)
            for (drawingInfo in mDrawingList!!) {
                drawingInfo.draw(mBufferCanvas)
            }
            invalidate()
        }
    }

    fun canRedo(): Boolean {
        return mRemovedList != null && mRemovedList!!.size > 0
    }

    fun canUndo(): Boolean {
        return mDrawingList != null && mDrawingList!!.size > 0
    }

    fun redo() {
        val size = if (mRemovedList == null) 0 else mRemovedList!!.size
        if (size > 0) {
            val info = mRemovedList!!.removeAt(size - 1)
            mDrawingList!!.add(info)
            mCanEraser = true
            reDraw()
            if (mCallback != null) {
                mCallback!!.onUndoRedoStatusChanged()
            }
        }
    }

    fun undo() {
        val size = if (mDrawingList == null) 0 else mDrawingList!!.size
        if (size > 0) {
            val info = mDrawingList!!.removeAt(size - 1)
            if (mRemovedList == null) {
                mRemovedList = ArrayList(MAX_CACHE_STEP)
            }
            if (size == 1) {
                mCanEraser = false
            }
            mRemovedList!!.add(info)
            reDraw()
            if (mCallback != null) {
                mCallback!!.onUndoRedoStatusChanged()
            }
        }
    }

    fun clear() {
        if (mBufferBitmap != null) {
            if (mDrawingList != null) {
                mDrawingList!!.clear()
            }
            if (mRemovedList != null) {
                mRemovedList!!.clear()
            }

            mCanEraser = false
            mBufferBitmap!!.eraseColor(Color.TRANSPARENT)
            invalidate()
            if (mCallback != null) {
                mCallback!!.onUndoRedoStatusChanged()
            }
            init()
            if (lastUsedImage != null) {
                setBack(lastUsedImage!!)
                mCanEraser = true
            }
        }
    }

    fun buildBitmap(): Bitmap {
        val bm = drawingCache
        val result = Bitmap.createBitmap(bm)
        destroyDrawingCache()
        return result
    }

    private fun saveDrawingPath() {
        if (mDrawingList == null) {
            mDrawingList = ArrayList(MAX_CACHE_STEP)
        } else if (mDrawingList!!.size == MAX_CACHE_STEP) {
            mDrawingList!!.removeAt(0)
        }
        val cachePath = Path(mPath)
        val cachePaint = Paint(mPaint)
        val info = PathDrawingInfo()
        info.path = cachePath
        info.paint = cachePaint
        mDrawingList!!.add(info)
        mCanEraser = true
        if (mCallback != null) {
            mCallback!!.onUndoRedoStatusChanged()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mBufferBitmap != null) {
            canvas.drawBitmap(mBufferBitmap!!, 0f, 0f, null)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEnabled) {
            return false
        }
        val action = event.action and MotionEvent.ACTION_MASK
        val x = event.x
        val y = event.y
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mLastX = x
                mLastY = y
                if (mPath == null) {
                    mPath = Path()
                }
                lastAction = MotionEvent.ACTION_DOWN
                mPath!!.moveTo(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                mPath!!.quadTo(mLastX, mLastY, (x + mLastX) / 2, (y + mLastY) / 2)
                if (mBufferBitmap == null) {
                    initBuffer()
                }
                if (mode == Mode.ERASER && !mCanEraser) {
                    //break
                    return false
                }
                lastAction = MotionEvent.ACTION_DOWN
                mBufferCanvas!!.drawPath(mPath!!, mPaint!!)
                invalidate()
                mLastX = x
                mLastY = y
            }
            MotionEvent.ACTION_UP -> {
                if (mode == Mode.DRAW || mCanEraser) {
                    saveDrawingPath()
                }
                mPath!!.reset()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                //Extract the index of the pointer that left the screen
                val pointerIndex = action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = event.getPointerId(pointerIndex)
            }//                if (pointerId == mActivePointerID) {
            //                    //Our active pointer is going up Choose another active pointer and adjust
            //                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            //                    mLastX = event.getX(newPointerIndex);
            //                    mLast = event.getY(newPointerIndex);
            //                    mActivePointerID = event.getPointerId(newPointerIndex);
            //                }
        }
        return true
    }

    companion object {

        private val MAX_CACHE_STEP = 20
    }
}
package com.wcjk.triage.common;

import android.annotation.TargetApi;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.DisplayMetrics;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import static android.widget.RelativeLayout.ALIGN_LEFT;

/**
 * Created by hyc on 2018/7/23
 */
public abstract class BaseDialog extends DialogFragment {

    @LayoutRes
    protected int mLayoutResId;

    private float mDimAmount = 0.8f;//背景昏暗度
    private int gravity = 0;
    private LayoutGravity layoutGravity;
    private View anchor;
    private int xmerge;
    private int ymerge;
    private int mMargin = 0;//左右边距
    private int mAnimStyle = 0;//进入退出动画
    private boolean mOutCancel = true;//点击外部取消
    public Context mContext;
    private int mWidth;
    private int mHeight;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle (DialogFragment.STYLE_NO_TITLE, 0);
        mLayoutResId = setUpLayoutId();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(mLayoutResId, container, false);
        convertView(ViewHolder.create(view), this);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        initParams();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void initParams() {
        Window window = getDialog().getWindow();
        if (window != null) {
            //防止背景出现黑框
            window.setBackgroundDrawable (new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params = window.getAttributes();
            params.dimAmount = mDimAmount;

            //设置dialog宽度
            if (mWidth == 0) {
                params.width =  998;
//                params.width = getScreenWidth(getContext()) - 2 * dp2px(getContext(), mMargin);
            } else {
//                params.width = dp2px(getContext(), mWidth);
                  params.width =  mWidth;

            }

            //设置dialog高度
            if (mHeight == 0) {
                params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            } else {
//                params.height = dp2px(getContext(), mHeight);
                params.height =  mHeight;
            }

            if (anchor != null){
                int[] offset=layoutGravity.getOffset(anchor, params);
                params.x = offset[0] + xmerge;
                params.y = offset[1] + ymerge;
            }else{
                params.gravity = gravity;
            }

            //设置dialog动画
            if (mAnimStyle != 0) {
                window.setWindowAnimations(mAnimStyle);
            }
            window.setAttributes(params);

        }
        setCancelable(mOutCancel);
    }

    /**
     * 设置背景昏暗度
     *
     * @param dimAmount alpha在0.0f到1.0f之间。1.0完全不透明，0.0f完全透明，自身不可见
     * @return
     */
    public BaseDialog setDimAmout(@FloatRange(from = 0, to = 1) float dimAmount) {
        mDimAmount = dimAmount;
        return this;
    }

    /**
     * 设置相对显示位置
     *
     * @param view 相对控件
     * @param gravity 布局
     * @param xmerge 相对x偏移
     * @param ymerge 相对y偏移
     * @return
     */
    public BaseDialog setGravity(View view, LayoutGravity gravity, int xmerge, int ymerge) {
        this.anchor = view;
        this.layoutGravity = gravity;
        this.xmerge = xmerge;
        this.ymerge = ymerge;
        return this;
    }

    /**
     * 设置显示位置
     *
     * @param gravity :Gravity.BOTTOM Gravity.TOP等等
     * @return
     */
    public BaseDialog setGravity(int gravity ) {
        this.gravity = gravity;
        return this;
    }

    /**
     * 设置宽高
     *
     * @param width
     * @param height
     * @return
     */
    public BaseDialog setSize(int width, int height) {
        mWidth = width;
        mHeight = height;
        return this;
    }

    /**
     * 设置左右margin
     *
     * @param margin
     * @return
     */
    public BaseDialog setMargin(int margin) {
        mMargin = margin;
        return this;
    }

    /**
     * 设置进入退出动画
     *
     * @param animStyle
     * @return
     */
    public BaseDialog setAnimStyle(@StyleRes int animStyle) {
        mAnimStyle = animStyle;
        return this;
    }

    /**
     * 设置是否点击外部取消
     *
     * @param outCancel
     * @return
     */
    public BaseDialog setOutCancel(boolean outCancel) {
        mOutCancel = outCancel;
        return this;
    }

    public BaseDialog show(FragmentManager manager) {
        super.show(manager, String.valueOf(System.currentTimeMillis()));
        return this;
    }

    /**
     * 设置dialog布局
     *
     * @return
     */
    public abstract int setUpLayoutId();

    /**
     * 操作dialog布局
     *
     * @param holder
     * @param dialog
     */
    public abstract void convertView(ViewHolder holder, BaseDialog dialog);

    /**
     * 获取屏幕宽度
     *
     * @param context
     * @return
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return displayMetrics.widthPixels;
    }

    public static int dp2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public static class LayoutGravity{
        private int layoutGravity;
        // waring, don't change the order of these constants!
        // public static final int ALIGN_LEFT=0x1;
        public static final int ALIGN_ABOVE=0x2;
        public static final int ALIGN_RIGHT=0x4;
        public static final int ALIGN_BOTTOM=0x8;
        public static final int TO_LEFT=0x10;
        public static final int TO_ABOVE=0x20;
        public static final int TO_RIGHT=0x40;
        public static final int TO_BOTTOM=0x80;
        public static final int CENTER_HORI=0x100;
        public static final int CENTER_VERT=0x200;

        public LayoutGravity(int gravity) {
            layoutGravity=gravity;
        }
        public int getLayoutGravity() {
            return layoutGravity;
        }
        public void setLayoutGravity(int gravity) {
            layoutGravity=gravity;
        }

        public void setHoriGravity(int gravity) {
            layoutGravity&=(0x2+0x8+0x20+0x80+0x200);
            layoutGravity|=gravity;
        }
        public void setVertGravity(int gravity) {
            layoutGravity&=(0x1+0x4+0x10+0x40+0x100);
            layoutGravity|=gravity;
        }

        public boolean isParamFit(int param) {
            return (layoutGravity & param) > 0;
        }

        public int getHoriParam() {
            for(int i=0x1; i<=0x100; i=i<<2)
                if(isParamFit(i))
                    return i;
            return ALIGN_LEFT;
        }

        public int getVertParam() {
            for(int i=0x2; i<=0x200; i=i<<2)
                if(isParamFit(i))
                    return i;
            return TO_BOTTOM;
        }

        public int[] getOffset(View anchor, WindowManager.LayoutParams params) {
            int anchWidth=anchor.getWidth();
            int anchHeight=anchor.getHeight();
            int winWidth=params.width;
            int winHeight=params.height;

            int xoff=0;
            int yoff=0;

            switch (getHoriParam()) {

                case ALIGN_LEFT:
                    xoff=0;
                    break;
                case ALIGN_RIGHT:
                    xoff=anchWidth-winWidth;
                    break;
                case TO_LEFT:
                    xoff=-winWidth;
                    break;
                case TO_RIGHT:
                    xoff=anchWidth; break;
                case CENTER_HORI:
                    xoff=(anchWidth-winWidth)/2; break;
                default:break;
            }

            switch (getVertParam()) {

                case ALIGN_ABOVE:
                    yoff=-anchHeight; break;
                case ALIGN_BOTTOM:
                    yoff=-winHeight; break;
                case TO_ABOVE:
                    yoff=-anchHeight-winHeight; break;
                case TO_BOTTOM:
                    yoff=0; break;
                case CENTER_VERT:
                    yoff=(-winHeight-anchHeight)/2; break;
                default:break;
            }

            return new int[]{ xoff, yoff };
        }
    }

    public static class ViewHolder {
        private SparseArray<View> views;
        private View convertView;

        private ViewHolder(View view) {
            convertView = view;
            views = new SparseArray<>();
        }

        public static ViewHolder create(View view) {
            return new ViewHolder(view);
        }

        /**
         * 获取View
         *
         * @param viewId
         * @param <T>
         * @return
         */
        public <T extends View> T getView(@IdRes int viewId) {
            View view = views.get(viewId);
            if (view == null) {
                view = convertView.findViewById(viewId);
                views.put(viewId, view);
            }
            return (T) view;
        }

        /**
         * 设置文本
         *
         * @param viewId
         * @param text
         */
        public void setText(int viewId, String text) {
            TextView textView = getView(viewId);
            textView.setText(text);
        }

        /**
         * 设置字体颜色
         *
         * @param viewId
         * @param colorId
         */
        public void setTextColor(int viewId, int colorId) {
            TextView textView = getView(viewId);
            textView.setTextColor(colorId);
        }

        /**
         * 设置背景图片
         *
         * @param viewId
         * @param resId
         */
        public void setBackgroundResource(int viewId, int resId) {
            View view = getView(viewId);
            view.setBackgroundResource(resId);
        }

        /**
         * 设置背景颜色
         *
         * @param viewId
         * @param colorId
         */
        public void setBackgroundColor(int viewId, int colorId) {
            View view = getView(viewId);
            view.setBackgroundColor(colorId);
        }

        /**
         * 设置点击事件
         *
         * @param viewId
         * @param listener
         */
        public void setOnClickListener(int viewId, View.OnClickListener listener) {
            View view = getView(viewId);
            view.setOnClickListener(listener);
        }
    }
}

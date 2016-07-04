package com.github.zeng1990java.cashier;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.widget.EditText;

/**
 *
 * Created by zengxiangbin on 2016/6/28.
 */
public class CashierEditText extends EditText{

    private static final boolean DEBUG = true;

    private static final int HINT_CURSOR_POINTER_PADDING = (int) (Resources.getSystem().getDisplayMetrics().density * 2);
    private static final int PREFIX_PADDING = (int) (Resources.getSystem().getDisplayMetrics().density * 3);

    private static final int MAX_PENNY = 9999999;

    private String mPrefixText = "￥";
    private String mHintText;

    private int mMaxPenny = MAX_PENNY;

    private CashierInputFilter mCashierInputFilter;
    private PrefixTextWatcher mPrefixTextWatcher;

    public CashierEditText(Context context) {
        super(context);
        init(context, null);
    }

    public CashierEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CashierEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CashierEditText(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CashierEditText);

        mMaxPenny = a.getInt(R.styleable.CashierEditText_cet_max_penny, MAX_PENNY);
        mPrefixText = a.getString(R.styleable.CashierEditText_cet_prefix_text);

        a.recycle();

        setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        setSingleLine(true);

        setFilters(new InputFilter[]{mCashierInputFilter = new CashierInputFilter(mMaxPenny)});
        addTextChangedListener(mPrefixTextWatcher = new PrefixTextWatcher(this, mMaxPenny));

        if (mPrefixText == null){
            mPrefixText = "";
        }

        mHintText = getHint().toString();
        setHint("");
    }


    public void setHintText(String hintText){
        this.mHintText = hintText;
        postInvalidate();
    }

    public void setHintText(int resId){
        this.mHintText = getResources().getString(resId);
        postInvalidate();
    }

    public void setMaxPenny(int maxPenny){
        mMaxPenny = maxPenny;
        mCashierInputFilter.setMaxPenny(mMaxPenny);
        mPrefixTextWatcher.setMaxPenny(mMaxPenny);
        setText("");
    }

    /**
     * 获取输入的钱，单位为分
     * @return
     */
    public long getCashPenny(){
        return parseToPenny(getText().toString());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        String text = getText().toString();

        int gravity = getGravity() & Gravity.HORIZONTAL_GRAVITY_MASK;

        if (TextUtils.isEmpty(text)){
            super.onDraw(canvas);
            if (gravity == Gravity.LEFT){
                canvas.save();
                canvas.translate(HINT_CURSOR_POINTER_PADDING, 0);
                canvas.drawText(mHintText, getPaddingLeft(), getBaseline(), getPaint());
                canvas.restore();
            }else if (gravity == Gravity.RIGHT){
//                canvas.drawText(mHintText,0, mHintText.length(), getWidth() - getLayout().getLineWidth(0)  + getScrollX(), getBaseline(), getPaint());
                float dx = getPaint().measureText(mHintText);
                canvas.drawText(mHintText, getWidth() - dx - getPaddingRight() - HINT_CURSOR_POINTER_PADDING + getScrollX(), getBaseline(), getPaint());
                log(String.format("right width=%d, layoutWidth=%d, sx=%d", getWidth() ,(int)getLayout().getLineWidth(0), getScrollX()));
            }else if(gravity == Gravity.CENTER_HORIZONTAL){
                float dx = getPaint().measureText(mHintText);
                canvas.drawText(mHintText, getWidth()/2 - dx/2 - getPaddingRight() - HINT_CURSOR_POINTER_PADDING + getScrollX(), getBaseline(), getPaint());
            }
        }else {
            if (gravity == Gravity.LEFT){
                float dx = getPaint().measureText(mPrefixText);
                canvas.save();
                canvas.translate(dx + PREFIX_PADDING, 0);
                super.onDraw(canvas);
                canvas.restore();

                drawPrefix(canvas, getPaddingLeft());
            } else if (gravity == Gravity.RIGHT){

                super.onDraw(canvas);

                if (!TextUtils.isEmpty(mPrefixText)){
                    float dx = getPaint().measureText(mPrefixText);

                    drawPrefix(canvas, getWidth() - getPaddingRight() - getLayout().getLineWidth(0) - dx - PREFIX_PADDING  + getScrollX());
                }
            }else if(gravity == Gravity.CENTER_HORIZONTAL){
                super.onDraw(canvas);
                if (!TextUtils.isEmpty(mPrefixText)){

                    float textWidth = getPaint().measureText(getText().toString());
                    float dx = getPaint().measureText(mPrefixText);

                    drawPrefix(canvas, getWidth()/2 - textWidth/2 - dx - PREFIX_PADDING + getScrollX());
                }
            }else {
                super.onDraw(canvas);
            }
        }
    }

    private void drawPrefix(Canvas canvas, float left){
        int alpha = getPaint().getAlpha();
        getPaint().setAlpha(alpha / 2);
        canvas.drawText(mPrefixText, left, getBaseline(), getPaint());
        getPaint().setAlpha(alpha);
    }

    static class PrefixTextWatcher implements TextWatcher{

        private int mMaxPenny;

        EditText mEditText;

        private String mOldText;
        private int mSelectionStart;

        public PrefixTextWatcher(EditText editText, int maxPenny){
            mEditText = editText;
            this.mMaxPenny = maxPenny;
        }

        public void setMaxPenny(int maxPenny) {
            mMaxPenny = maxPenny;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            log("beforeTextChanged: "+String.format("%s, %d, %d, %d", charSequence.toString(),i, i1, i2));
            mOldText = charSequence.toString();
            mSelectionStart = i + i2;
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
//            log("onTextChanged: "+String.format("%s, %d, %d, %d", charSequence.toString(),i, i1, i2));
        }

        @Override
        public void afterTextChanged(Editable editable) {
//            log("afterTextChanged: "+editable.toString());
            mEditText.removeTextChangedListener(this);

            String text = editable.toString();
            long penny = parseToPenny(text);
            if (penny > mMaxPenny){
                mEditText.setText(mOldText);
            }else {
                if (text.startsWith("0")){
                    if (text.length() == 2){
                        if (!TextUtils.equals(text.charAt(1) +"" , ".")){
                            mEditText.setText(pennyToString(penny));
                        }
                    }if (text.length() > 2){
                        String newText = text;
                        if (text.indexOf(".") == -1){
                            mEditText.setText(pennyToString(penny));
                        }else {
                            while (newText.startsWith("0") && !newText.startsWith("0.")){
                                newText = newText.substring(1);
                            }
                            mEditText.setText(newText);
                        }
                    }
                }
            }
            String resultText = mEditText.getText().toString();
            if (resultText.startsWith(".")){
                resultText = "0" + resultText;
                if (mSelectionStart == 0) {
                    mSelectionStart = 1;
                }
            }

            mOldText = resultText;
            mEditText.setText(resultText);
            mEditText.setSelection(Math.min(mSelectionStart, mEditText.getText().length()));
            mEditText.addTextChangedListener(this);
        }
    }

    private static long parseToPenny(String text){
        return MoneyUtils.parseToPenny(text);
    }



    private static String pennyToString(long penney){
        return MoneyUtils.pennyToString(penney);
    }


    private static void log(String msg){
        if (DEBUG) {
            Log.d("CashEditText", msg);
        }
    }
}

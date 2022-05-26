/**
 * The MIT License (MIT) Copyright (c) 2015 OriginQiu Permission is hereby
 * granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of
 * the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO
 * EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */
package com.beastblocks.edittag;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.beastblocks.edittag.R;


/**
 * Created by OriginQiu on 4/7/16.
 */
public class EditTag extends FrameLayout
        implements View.OnClickListener, TextView.OnEditorActionListener, View.OnKeyListener {

    private FlowLayout flowLayout;

    private EditText editText;

    private int tagViewLayoutRes;
    private boolean alwaysvisible=false;

    private int inputTagLayoutRes;

    private int deleteModeBgRes;

    private Drawable defaultTagBg;

    private boolean isEditableStatus = true;

    private TextView lastSelectTagView;

    private List<String> tagValueList = new ArrayList<>();

    private boolean isDelAction = false;

    private TagAddCallback tagAddCallBack;

    private TagDeletedCallback tagDeletedCallback;

    public interface TagAddCallback {
        /*
         * Called when add a tag
         * true: tag would be added
         * false: tag would not be added
         */
        boolean onTagAdd(String tagValue);
    }

    public interface TagDeletedCallback {
        /**
         * Called when tag be deleted
         *
         * @param deletedTagValue
         */
        void onTagDelete(String deletedTagValue);
    }

    public EditTag(Context context) {
        this(context, null);
    }

    public EditTag(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EditTag(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.EditTag);
        tagViewLayoutRes =
                mTypedArray.getResourceId(R.styleable.EditTag_tag_layout, R.layout.view_default_tag);
        inputTagLayoutRes = mTypedArray.getResourceId(R.styleable.EditTag_input_layout,
                R.layout.view_default_input_tag);
        deleteModeBgRes =
                mTypedArray.getResourceId(R.styleable.EditTag_delete_mode_bg, R.color.colorAccent);
        mTypedArray.recycle();
        setupView();
    }

    public void setCancelButtonVisibility(Boolean visibility){
        alwaysvisible=visibility;
    }

    private void setupView() {
        flowLayout = new FlowLayout(getContext());
        LayoutParams layoutParams =
                new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        flowLayout.setLayoutParams(layoutParams);
        addView(flowLayout);
        addInputTagView();
    }

    private void addInputTagView() {
        editText = createInputTag(flowLayout);
        editText.setTag(new Object());
        editText.setOnClickListener(this);
        setupListener();
        flowLayout.addView(editText);
        isEditableStatus = true;
    }

    private void setupListener() {
        editText.setOnEditorActionListener(this);
        editText.setOnKeyListener(this);
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        boolean isHandle = false;
        if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
            String tagContent = editText.getText().toString();
            if (TextUtils.isEmpty(tagContent)) {
                int tagCount = flowLayout.getChildCount();
                if (lastSelectTagView == null && tagCount > 1) {
                    if (isDelAction) {
                        flowLayout.removeViewAt(tagCount - 2);
                        if (tagDeletedCallback != null) {
                            tagDeletedCallback.onTagDelete(tagValueList.get(tagCount - 2));
                        }
                        tagValueList.remove(tagCount - 2);
                        isHandle = true;
                    } else {
                        TextView delActionTagView = (TextView) flowLayout.getChildAt(tagCount - 2);
                        delActionTagView.setBackgroundDrawable(getDrawableByResId(deleteModeBgRes));
                        if (!alwaysvisible) {
                            for (int i = 0; i < (flowLayout.getChildCount()); i++) {
                                if (flowLayout.getChildAt(i) == delActionTagView) {
                                    ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_cancel_24, 0);
                                    ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(8);

                                } else {
                                    ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                                    ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(0);
                                }
                            }
                        }
                        lastSelectTagView = delActionTagView;
                        isDelAction = true;
                    }
                } else {
                    removeSelectedTag();
                }
            } else {
                int length = tagContent.length();
                editText.getText().delete(length, length);
            }
        }
        return isHandle;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        boolean isHandle = false;
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String tagContent = editText.getText().toString();
            if (TextUtils.isEmpty(tagContent)) {
                // do nothing, or you can tip "can'nt add empty tag"
            } else {
                if (tagAddCallBack == null || (tagAddCallBack != null
                        && tagAddCallBack.onTagAdd(tagContent))) {
                    TextView tagTextView = createTag(flowLayout, tagContent);
                    if (defaultTagBg == null) {
                        defaultTagBg = tagTextView.getBackground();
                    }
                    tagTextView.setOnClickListener(EditTag.this);
                        tagTextView.setOnTouchListener(new OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                Rect bounds;
                                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                    int actionX = (int) motionEvent.getX();
                                    int actionY = (int) motionEvent.getY();
                                    Drawable drawableRight = tagTextView.getCompoundDrawables()[2];
                                    if (drawableRight != null) {

                                        bounds = null;
                                        bounds = drawableRight.getBounds();

                                        int x, y;
                                        int extraTapArea = 13;

                                        /**
                                         * IF USER CLICKS JUST OUT SIDE THE RECTANGLE OF THE DRAWABLE
                                         * THAN ADD X AND SUBTRACT THE Y WITH SOME VALUE SO THAT AFTER
                                         * CALCULATING X AND Y CO-ORDINATE LIES INTO THE DRAWBABLE
                                         * BOUND. - this process help to increase the tappable area of
                                         * the rectangle.
                                         */
                                        x = (int) (actionX + extraTapArea);
                                        y = (int) (actionY - extraTapArea);

                                        /**Since this is right drawable subtract the value of x from the width
                                         * of view. so that width - tappedarea will result in x co-ordinate in drawable bound.
                                         */
                                        x = tagTextView.getWidth() - x;

                                        /*x can be negative if user taps at x co-ordinate just near the width.
                                         * e.g views width = 300 and user taps 290. Then as per previous calculation
                                         * 290 + 13 = 303. So subtract X from getWidth() will result in negative value.
                                         * So to avoid this add the value previous added when x goes negative.
                                         */

                                        if (x <= 0) {
                                            x += extraTapArea;
                                        }

                                        /* If result after calculating for extra tappable area is negative.
                                         * assign the original value so that after subtracting
                                         * extratapping area value doesn't go into negative value.
                                         */

                                        if (y <= 0)
                                            y = actionY;

                                        /**If drawble bounds contains the x and y points then move ahead.*/
                                        if (bounds.contains(x, y)) {
                                            lastSelectTagView = tagTextView;
                                            isDelAction = true;
                                            removeSelectedTag();
                                            return false;
                                        }
                                        return EditTag.super.onTouchEvent(motionEvent);
                                    }

                                }
                                return EditTag.super.onTouchEvent(motionEvent);
                            }
                        });
                    flowLayout.addView(tagTextView, flowLayout.getChildCount() - 1);
                    tagValueList.add(tagContent);
                    // reset action status
                    editText.getText().clear();
                    editText.performClick();
                    isDelAction = false;
                    isHandle = true;
                }
            }
        }
        return isHandle;
    }

    @Override
    public void onClick(View view) {
        if (view.getTag() == null && isEditableStatus) {
            // TextView tag click
            if (lastSelectTagView == null) {
                lastSelectTagView = (TextView) view;
                view.setBackgroundDrawable(getDrawableByResId(deleteModeBgRes));
                if (!alwaysvisible) {
                    for (int i = 0; i < (flowLayout.getChildCount()); i++) {
                        if (flowLayout.getChildAt(i) == view) {
                            ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_cancel_24, 0);
                            ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(8);

                        } else {
                            ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                            ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(0);
                        }
                    }
                }
            } else {
                if (lastSelectTagView.equals(view)) {
                    lastSelectTagView.setBackgroundDrawable(defaultTagBg);
                    lastSelectTagView = null;
                    if (!alwaysvisible) {
                        ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        ((TextView) view).setCompoundDrawablePadding(0);
                    }
                } else {
                    lastSelectTagView.setBackgroundDrawable(defaultTagBg);
                    lastSelectTagView = (TextView) view;
                    view.setBackgroundDrawable(getDrawableByResId(deleteModeBgRes));
                    if (!alwaysvisible) {
                        for (int i = 0; i < (flowLayout.getChildCount()); i++) {
                            if (flowLayout.getChildAt(i) == view) {
                                ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_cancel_24, 0);
                                ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(8);

                            } else {
                                ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                                ((TextView) flowLayout.getChildAt(i)).setCompoundDrawablePadding(0);
                            }
                        }
                    }
                }
            }
        } else {
            // EditText tag click
            if (lastSelectTagView != null) {
                lastSelectTagView.setBackgroundDrawable(defaultTagBg);
                lastSelectTagView = null;
            }
        }
    }

    private void removeSelectedTag() {
        int size = tagValueList.size();
        if (size > 0 && lastSelectTagView != null) {
            int index = flowLayout.indexOfChild(lastSelectTagView);
            tagValueList.remove(index);
            flowLayout.removeView(lastSelectTagView);
            if (tagDeletedCallback != null) {
                tagDeletedCallback.onTagDelete(lastSelectTagView.getText().toString());
            }
            lastSelectTagView = null;
            isDelAction = false;
        }
    }

    private TextView createTag(ViewGroup parent, String s) {
        TextView tagTv = (TextView) LayoutInflater.from(getContext()).inflate(tagViewLayoutRes, parent, false);
        //TextView tagTv = new TextView(getContext());
        if (alwaysvisible) {
            tagTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_baseline_cancel_24, 0);
            tagTv.setCompoundDrawablePadding(8);
        }
        tagTv.setText(s);
        return tagTv;
    }

    private EditText createInputTag(ViewGroup parent) {
        editText =
                (EditText) LayoutInflater.from(getContext()).inflate(inputTagLayoutRes, parent, false);
        return editText;
    }

    private void addTagView(List<String> tagList) {
        int size = tagList.size();
        for (int i = 0; i < size; i++) {
            addTag(tagList.get(i));
        }
    }

    private Drawable getDrawableByResId(int resId) {
        return getContext().getResources().getDrawable(resId);
    }

    public void setEditable(boolean editable) {
        if (editable) {
            if (!isEditableStatus) {
                flowLayout.addView((editText));
            }
        } else {
            int childCount = flowLayout.getChildCount();
            if (isEditableStatus && childCount > 0) {
                flowLayout.removeViewAt(childCount - 1);
                if (lastSelectTagView != null) {
                    lastSelectTagView.setBackgroundDrawable(defaultTagBg);
                    isDelAction = false;
                    editText.getText().clear();
                }
            }
        }
        this.isEditableStatus = editable;
    }

    public boolean addTag(String tagContent) {
        if (TextUtils.isEmpty(tagContent)) {
            // do nothing, or you can tip "can't add empty tag"
            return false;
        } else {
            if (tagAddCallBack == null || (tagAddCallBack != null
                    && tagAddCallBack.onTagAdd(tagContent))) {
                TextView tagTextView = createTag(flowLayout, tagContent);
                if (defaultTagBg == null) {
                    defaultTagBg = tagTextView.getBackground();
                }
                tagTextView.setOnClickListener(EditTag.this);
                    tagTextView.setOnTouchListener(new OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            Rect bounds;
                            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                                int actionX = (int) motionEvent.getX();
                                int actionY = (int) motionEvent.getY();
                                Drawable drawableRight = tagTextView.getCompoundDrawables()[2];
                                if (drawableRight != null) {

                                    bounds = null;
                                    bounds = drawableRight.getBounds();

                                    int x, y;
                                    int extraTapArea = 13;

                                    /**
                                     * IF USER CLICKS JUST OUT SIDE THE RECTANGLE OF THE DRAWABLE
                                     * THAN ADD X AND SUBTRACT THE Y WITH SOME VALUE SO THAT AFTER
                                     * CALCULATING X AND Y CO-ORDINATE LIES INTO THE DRAWBABLE
                                     * BOUND. - this process help to increase the tappable area of
                                     * the rectangle.
                                     */
                                    x = (int) (actionX + extraTapArea);
                                    y = (int) (actionY - extraTapArea);

                                    /**Since this is right drawable subtract the value of x from the width
                                     * of view. so that width - tappedarea will result in x co-ordinate in drawable bound.
                                     */
                                    x = tagTextView.getWidth() - x;

                                    /*x can be negative if user taps at x co-ordinate just near the width.
                                     * e.g views width = 300 and user taps 290. Then as per previous calculation
                                     * 290 + 13 = 303. So subtract X from getWidth() will result in negative value.
                                     * So to avoid this add the value previous added when x goes negative.
                                     */

                                    if (x <= 0) {
                                        x += extraTapArea;
                                    }

                                    /* If result after calculating for extra tappable area is negative.
                                     * assign the original value so that after subtracting
                                     * extratapping area value doesn't go into negative value.
                                     */

                                    if (y <= 0)
                                        y = actionY;

                                    /**If drawble bounds contains the x and y points then move ahead.*/
                                    if (bounds.contains(x, y)) {
                                        lastSelectTagView = tagTextView;
                                        isDelAction = true;
                                        removeSelectedTag();
                                        return false;
                                    }
                                    return EditTag.super.onTouchEvent(motionEvent);
                                }

                            }
                            return EditTag.super.onTouchEvent(motionEvent);
                        }
                    });
                if (isEditableStatus) {
                    flowLayout.addView(tagTextView, flowLayout.getChildCount() - 1);
                } else {
                    flowLayout.addView(tagTextView);
                }

                tagValueList.add(tagContent);
                // reset action status
                editText.getText().clear();
                editText.performClick();
                isDelAction = false;
                return true;
            }
        }
        return false;
    }

    public void setTagList(List<String> mTagList) {
        addTagView(mTagList);
    }

    public List<String> getTagList() {
        return tagValueList;
    }

    public void setTagAddCallBack(TagAddCallback tagAddCallBack) {
        this.tagAddCallBack = tagAddCallBack;
    }

    public void setTagDeletedCallback(TagDeletedCallback tagDeletedCallback) {
        this.tagDeletedCallback = tagDeletedCallback;
    }

    /*
     * Remove tag view by value
     * warning: this method will remove tags which has the same value
     */
    public void removeTag(String... tagValue) {
        List<String> tagValues = Arrays.asList(tagValue);
        int childCount = flowLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            if (tagValues.size() > 0) {
                View view = flowLayout.getChildAt(i);
                try {
                    String value = ((TextView) view).getText().toString();
                    if (tagValues.contains(value)) {
                        tagValueList.remove(value);
                        if (tagDeletedCallback != null) {
                            tagDeletedCallback.onTagDelete(value);
                        }
                        flowLayout.removeView(view);
                        i = 0;
                        childCount = flowLayout.getChildCount();
                        continue;
                    }
                } catch (ClassCastException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }
}

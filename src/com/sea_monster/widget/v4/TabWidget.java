package com.sea_monster.widget.v4;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.LinearLayout;


public class TabWidget extends LinearLayout implements OnFocusChangeListener {

	private OnTabSelectionChanged selectionChangedListener;
	private String selectedTab;

	public TabWidget(Context context) {
		this(context, null);
	}

	public TabWidget(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTabWidget();
	}

	private void initTabWidget() {
		setFocusable(true);
		setOnFocusChangeListener(this);
	}

	void setTabSelectionListener(OnTabSelectionChanged listener) {
		selectionChangedListener = listener;
	}

	public View getChildTabViewAt(String tag) {
		return findViewWithTag(tag);
	}

	public void setCurrentTab(String tag) {
		if (tag == null) {
			return;
		}
		if (selectedTab != null)
			getChildTabViewAt(selectedTab).setSelected(false);

		selectedTab = tag;
		getChildTabViewAt(selectedTab).setSelected(true);
	}

	public void focusCurrentTab(String tag) {
		final String oldTab = selectedTab;
		setCurrentTab(tag);
		if (oldTab != tag) {
			findViewWithTag(tag).requestFocus();
		}
	}

	public int getTabCount() {
		int children = getChildCount();
		return children;
	}

	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		int count = getTabCount();

		for (int i = 0; i < count; i++) {
			View child = getChildAt(i);
			child.setEnabled(enabled);
		}
	}

	public void addView(View child) {

		child.setFocusable(true);
		child.setClickable(true);

		super.addView(child);

		child.setOnClickListener(new TabClickListener((String) child.getTag()));
		child.setOnFocusChangeListener(this);
	}

	public void addView(View child, int index) {

		child.setFocusable(true);
		child.setClickable(true);

		super.addView(child, index);
		child.setOnClickListener(new TabClickListener((String) child.getTag()));
		child.setOnFocusChangeListener(this);
	}

	public void childDrawableStateChanged(View child) {
		if (child == getChildTabViewAt(selectedTab)) {
			invalidate();
		}
		super.childDrawableStateChanged(child);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

		super.onLayout(changed, left, top, right, bottom);
	}

	// registered with each tab indicator so we can notify tab host
	private class TabClickListener implements OnClickListener {

		private final String mTag;

		private TabClickListener(String tag) {
			this.mTag = tag;
		}

		public void onClick(View v) {
			if (!selectionChangedListener.onTabSelectionClick(mTag))
				selectionChangedListener.onTabSelectionChanged(mTag, true);
		}
	}

	static interface OnTabSelectionChanged {
		void onTabSelectionChanged(String tag, boolean clicked);

		boolean onTabSelectionClick(String tag);
	}

	public void onFocusChange(View v, boolean hasFocus) {
		if (v == this && hasFocus) {
			getChildTabViewAt(selectedTab).requestFocus();
			return;
		}

		if (hasFocus) {
			int i = 0;
			int numTabs = getTabCount();
			while (i < numTabs) {
				if (getChildAt(i) == v) {
					setCurrentTab((String) v.getTag());
					selectionChangedListener.onTabSelectionChanged((String) v.getTag(), false);
					break;
				}
				i++;
			}
		}
	}

}

package com.sea_monster.widget.v4;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

public class TabHost extends FrameLayout implements ViewTreeObserver.OnTouchModeChangeListener {
	private TabWidget mTabWidget;
	private FrameLayout mTabContent;
	private Map<String, TabSpec> mTabSpecs = new HashMap<String, TabSpec>();
	protected String mCurrentTab = null;
	protected String mHistoryTab = null;
	protected FragmentManager mFragmentManager = null;
	private OnTabChangeListener mOnTabChangeListener;
	private OnKeyListener mTabKeyListener;

	int mTabEnterAnim, mTabExitAnim;

	public TabHost(Context context) {
		super(context);
		initTabHost();
	}

	public TabHost(Context context, AttributeSet attrs) {
		super(context, attrs);
		initTabHost();
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TabHost);
		mTabEnterAnim = a.getResourceId(R.styleable.TabHost_tab_enter_anim, 0);
		mTabExitAnim = a.getInt(R.styleable.TabHost_tab_exit_anim, 0);

		a.recycle();
	}

	private void initTabHost() {
		setFocusableInTouchMode(true);
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);

		mCurrentTab = null;
	}

	public TabSpec newTabSpec(String tag) {
		return new TabSpec(tag);
	}

	public void setup() {
		mTabWidget = (TabWidget) findViewById(android.R.id.tabs);
		if (mTabWidget == null) {
			throw new RuntimeException("Your TabHost must have a TabWidget whose id attribute is 'android.R.id.tabs'");
		}

		// KeyListener to attach to all tabs. Detects non-navigation keys
		// and relays them to the tab content.
		mTabKeyListener = new OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_CENTER:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_ENTER:
					return false;

				}
				mTabContent.requestFocus(View.FOCUS_FORWARD);
				return mTabContent.dispatchKeyEvent(event);
			}

		};

		mTabWidget.setTabSelectionListener(new TabWidget.OnTabSelectionChanged() {
			public void onTabSelectionChanged(String tag, boolean clicked) {
				setCurrentTab(tag);
				if (clicked) {
					mTabContent.requestFocus(View.FOCUS_FORWARD);
				}
			}

			@Override
			public boolean onTabSelectionClick(String tag) {
				return mOnTabChangeListener.onTabClicked(tag);
			}
		});

		mTabContent = (FrameLayout) findViewById(android.R.id.tabcontent);
		if (mTabContent == null) {
			throw new RuntimeException("Your TabHost must have a FrameLayout whose id attribute is "
					+ "'android.R.id.tabcontent'");
		}
	}

	public void setup(FragmentManager fragmentManager) {
		setup();
		mFragmentManager = fragmentManager;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		final ViewTreeObserver treeObserver = getViewTreeObserver();
		if (treeObserver != null) {
			treeObserver.addOnTouchModeChangeListener(this);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		final ViewTreeObserver treeObserver = getViewTreeObserver();
		if (treeObserver != null) {
			treeObserver.removeOnTouchModeChangeListener(this);
		}
	}

	public void onTouchModeChanged(boolean isInTouchMode) {
		if (!isInTouchMode) {
			// leaving touch mode.. if nothing has focus, let's give it to
			// the indicator of the current tab
			if (mTabContent != null && (!mTabContent.hasFocus() || mTabContent.isFocused())) {
				mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
			}
		}
	}

	public void addTab(TabSpec tabSpec) {

		if (tabSpec.mIndicatorStrategy == null) {
			throw new IllegalArgumentException("you must specify a way to create the tab indicator.");
		}

		// if (tabSpec.mFragment == null) {
		// throw new
		// IllegalArgumentException("you must specify a way to create the tab content");
		// }
		View tabIndicator = tabSpec.mIndicatorStrategy.createIndicatorView();
		tabIndicator.setTag(tabSpec.mTag);
		tabIndicator.setOnKeyListener(mTabKeyListener);

		mTabWidget.addView(tabIndicator);
		mTabSpecs.put(tabSpec.mTag, tabSpec);

		/*
		 * if (mCurrentTab == -1) { setCurrentTab(0); }
		 */
	}

	/**
	 * Removes all tabs from the tab widget associated with this tab host.
	 */
	public void clearAllTabs() {
		mTabWidget.removeAllViews();
		initTabHost();
		mTabContent.removeAllViews();
		mTabSpecs.clear();
		requestLayout();
		invalidate();
	}

	public void clearWidget() {
		mTabWidget.removeAllViews();
		mTabSpecs.clear();
	}

	public TabSpec getCurrentTabSpec() {
		return mTabSpecs.get(mCurrentTab);
	}

	public TabWidget getTabWidget() {
		return mTabWidget;
	}

	public String getCurrentTab() {
		return mCurrentTab;
	}

	public int getCurrentIndex() {
		int i = 0;

		for (; i < mTabWidget.getChildCount(); i++) {
			Object object = mTabWidget.getChildAt(i).getTag();
			if (object != null & mCurrentTab.equals(object))
				break;
		}

		return i;
	}

	public View getCurrentTabView() {
		if (mCurrentTab != null) {
			return mTabWidget.getChildTabViewAt(mCurrentTab);
		}
		return null;
	}

	public View getCurrentView() {
		return mTabContent;
	}

	public FrameLayout getTabContentView() {
		return mTabContent;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		final boolean handled = super.dispatchKeyEvent(event);

		if (!handled && (event.getAction() == KeyEvent.ACTION_DOWN) && (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_UP)
				&& (mTabContent.hasFocus()) && (mTabContent.findFocus().focusSearch(View.FOCUS_UP) == null)) {
			mTabWidget.getChildTabViewAt(mCurrentTab).requestFocus();
			playSoundEffect(SoundEffectConstants.NAVIGATION_UP);
			return true;
		}
		return handled;
	}

	@Override
	public void dispatchWindowFocusChanged(boolean hasFocus) {
		mTabContent.dispatchWindowFocusChanged(hasFocus);
	}

	public void setCurrentTab(String tag) {
		if (tag == null) {
			return;
		}

		if (tag == mCurrentTab) {
			mTabWidget.focusCurrentTab(mCurrentTab);
			return;
		}

		FragmentTransaction transaction = mFragmentManager.beginTransaction();

		if (mCurrentTab != null) {

			for (TabSpec spec : mTabSpecs.values()) {
				if (spec.mFragment != null && spec.mFragment.isAdded()) {
					if (mTabEnterAnim != 0 && mTabExitAnim != 0)
						transaction.setCustomAnimations(mTabEnterAnim, mTabExitAnim);
					transaction.detach(spec.mFragment);
					transaction.commit();
					transaction = mFragmentManager.beginTransaction();
				}
			}

		}

		mCurrentTab = tag;
		final TabSpec spec = mTabSpecs.get(tag);

		mTabWidget.focusCurrentTab(mCurrentTab);
		if (mTabEnterAnim != 0 && mTabExitAnim != 0)
			transaction.setCustomAnimations(mTabEnterAnim, mTabExitAnim);
		if (!spec.mFragment.isDetached() && !spec.mFragment.isAdded()) {
			transaction.add(android.R.id.tabcontent, spec.mFragment, spec.mTag);
		} else {
			transaction.attach(spec.mFragment);
		}
		transaction.commit();

		mTabContent = (FrameLayout) findViewById(android.R.id.tabcontent);

		if (!mTabWidget.hasFocus()) {
			mTabContent.requestFocus();
		}

		invokeOnTabChangeListener();
	}

	/**
	 * Register a callback to be invoked when the selected state of any of the
	 * items in this list changes
	 * 
	 * @param l
	 *            The callback that will run
	 */
	public void setOnTabChangedListener(OnTabChangeListener l) {
		mOnTabChangeListener = l;
	}

	private void invokeOnTabChangeListener() {
		if (mOnTabChangeListener != null) {
			mOnTabChangeListener.onTabChanged(mCurrentTab);
		}
	}

	/**
	 * Interface definition for a callback to be invoked when tab changed
	 */
	public interface OnTabChangeListener {
		void onTabChanged(String tabId);

		boolean onTabClicked(String tabId);
	}

	public interface TabContentFactory {

		View createTabContent(String tag);
	}

	public class TabSpec {

		private String mTag;
		private IndicatorStrategy mIndicatorStrategy;
		private Fragment mFragment;

		private TabSpec(String tag) {
			mTag = tag;
		}

		public TabSpec setIndicator(CharSequence label, Drawable icon) {
			mIndicatorStrategy = new LabelAndIconIndicatorStrategy(label, icon);
			return this;
		}

		public TabSpec setIndicator(Drawable icon) {
			mIndicatorStrategy = new IconIndicatorStrategy(icon);
			return this;
		}

		public TabSpec setIndicator(View view) {
			mIndicatorStrategy = new ViewIndicatorStrategy(view);
			return this;
		}

		public TabSpec setContent(Fragment fragment) {
			mFragment = fragment;
			return this;
		}

		public TabSpec updateIndicator(Drawable icon) {
			if (mIndicatorStrategy != null && mIndicatorStrategy instanceof IconIndicatorStrategy) {
				((IconIndicatorStrategy) mIndicatorStrategy).updateIndicatorStrategy(icon);
			} else {
				throw new RuntimeException("IndicatorStrategy Not Init");
			}
			return this;
		}

		public TabSpec updateIndicator(CharSequence lable, Drawable icon) {
			if (mIndicatorStrategy != null && mIndicatorStrategy instanceof LabelAndIconIndicatorStrategy) {
				((LabelAndIconIndicatorStrategy) mIndicatorStrategy).updateIndicatorStrategy(lable, icon);
			} else {
				throw new RuntimeException("IndicatorStrategy Not Init");
			}
			return this;
		}

		public TabSpec updateIndicator(View view) {
			if (mIndicatorStrategy != null && mIndicatorStrategy instanceof ViewIndicatorStrategy) {
				((ViewIndicatorStrategy) mIndicatorStrategy).updateIndicatorStrategy(view);
			} else {
				throw new RuntimeException("IndicatorStrategy Not Init");
			}
			return this;
		}

		public Fragment getFragment() {
			return mFragment;
		}

		public String getTag() {
			return mTag;
		}

	}

	private static interface IndicatorStrategy {
		View createIndicatorView();
	}

	private class LabelAndIconIndicatorStrategy implements IndicatorStrategy {

		private CharSequence mLabel;
		private Drawable mIcon;
		private View mIndicatorView;

		private LabelAndIconIndicatorStrategy(CharSequence label, Drawable icon) {
			mLabel = label;
			mIcon = icon;
		}

		public View createIndicatorView() {
			final Context context = getContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mIndicatorView = inflater.inflate(R.layout.ui_tab_indicator, mTabWidget, false);

			final TextView tv = (TextView) mIndicatorView.findViewById(android.R.id.title);
			tv.setText(mLabel);
			mIcon.setBounds(0, 0, mIcon.getMinimumWidth(), mIcon.getMinimumHeight());
			tv.setCompoundDrawables(null, mIcon, null, null);
			return mIndicatorView;
		}

		void updateIndicatorStrategy(CharSequence lable, Drawable icon) {
			mIcon = icon;
			mLabel = lable;
			if (mIndicatorView == null)
				createIndicatorView();

			final TextView tv = (TextView) mIndicatorView.findViewById(android.R.id.title);
			tv.setText(mLabel);
			mIcon.setBounds(0, 0, mIcon.getMinimumWidth(), mIcon.getMinimumHeight());
			tv.setCompoundDrawables(null, mIcon, null, null);
		}

	}

	private class IconIndicatorStrategy implements IndicatorStrategy {

		private Drawable mIcon;
		private View mIndicatorView;

		private IconIndicatorStrategy(Drawable icon) {
			mIcon = icon;
		}

		public View createIndicatorView() {
			final Context context = getContext();
			LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			mIndicatorView = inflater.inflate(R.layout.ui_tab_indicator_icon, mTabWidget, false);

			final ImageButton iv = (ImageButton) mIndicatorView.findViewById(android.R.id.icon);
			iv.setImageDrawable(mIcon);
			return mIndicatorView;
		}

		void updateIndicatorStrategy(Drawable icon) {
			mIcon = icon;
			if (mIndicatorView == null)
				createIndicatorView();

			final ImageButton iv = (ImageButton) mIndicatorView.findViewById(android.R.id.icon);
			iv.setImageDrawable(mIcon);
		}

	}

	private class ViewIndicatorStrategy implements IndicatorStrategy {

		private View mView;

		private ViewIndicatorStrategy(View view) {
			mView = view;
		}

		public View createIndicatorView() {
			return mView;
		}

		void updateIndicatorStrategy(View view) {
			mView = view;
		}

	}

}

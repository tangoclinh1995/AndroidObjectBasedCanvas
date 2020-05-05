package tnl.objcanvas;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

/**
 * Created by Linh Ta on 1/9/2018.
 */

public class LinearButtonListLayout extends LinearLayout {
	public static final String TAG = "LinearButtonListLayout";



	public static final boolean DEFAULT_SHOW_BUTTON_TEXT = false;
	public static final boolean DEFAULT_SHOW_BUTTON_ICON = true;
	public static final int DEFAULT_BUTTON_ICON_SIZE_DP = 40;

	private static final int BUTTON_ID_OFFSET = 4;



	private int defaultButtonIconSizePixel;

	private int countButton = 0;
	private String[] titles = new String[0];
	private Drawable[] icons = new Drawable[0];

	private Button[] buttons;

	private boolean showButtonText = DEFAULT_SHOW_BUTTON_TEXT;
	private boolean showButtonIcon = DEFAULT_SHOW_BUTTON_ICON;

	private MyOnClickListener myOnClickListener = new MyOnClickListener();
	private OnButtonClickListener clickListener = null;



	public LinearButtonListLayout(Context context) {
		super(context);

		defaultButtonIconSizePixel = (int)(
			context.getResources().getDisplayMetrics().density
			* DEFAULT_BUTTON_ICON_SIZE_DP
			+ 0.5
		);

	}


	/**
	 *
	 * @param titles Can left null to indicate not using titles
	 * @param drawableIds Can left null to indicate not using icons
	 * @throws IllegalArgumentException
	 */
	public void setButtons(String[] titles, int[] drawableIds)
	throws IllegalArgumentException {
		countButton = 0;

		if (titles != null) {
			countButton = Math.max(countButton, titles.length);
		}

		if (drawableIds != null) {
			countButton = Math.max(countButton, drawableIds.length);
		}

		this.titles = new String[countButton];
		this.icons = new Drawable[countButton];

		for (int i = 0; i < countButton; ++i) {
			if (i < titles.length) {
				this.titles[i] = titles[i];
			} else {
				this.titles[i] = "";
			}

			this.icons[i] = null;

			if (i < icons.length) {
				try {
					this.icons[i] = getResources().getDrawable(drawableIds[i]);

					int iconWidth = this.icons[i].getIntrinsicWidth();
					int iconHeight = this.icons[i].getIntrinsicHeight();

					double scale
						= defaultButtonIconSizePixel
						/ (double)Math.max(iconWidth, iconHeight);

					this.icons[i].setBounds(
						0,
						0,
						(int)Math.floor(iconWidth * scale),
						(int)Math.floor(iconHeight * scale)
					);

				} catch (Resources.NotFoundException e) {
					this.icons[i] = null;

					throw new IllegalArgumentException(
						String.format(
							"The resource ID located at drawableIds[%d] not found",
							drawableIds[i]
						)

					);

				}

			}

		}

		removeAllViews();

		buttons = new Button[countButton];

		for (int i = 0; i < countButton; ++i) {
			buttons[i] = makeButton();

			// ID is used to detect button in its OnClickListener emission
			buttons[i].setId(i + BUTTON_ID_OFFSET);
			buttons[i].setOnClickListener(myOnClickListener);

			addView(buttons[i]);
		}

		changeButtonLayoutParams();
		changeShowButtonTextIcon();

		requestLayout();
	}


	public void setOnButtonClickListener(OnButtonClickListener clickListener) {
		this.clickListener = clickListener;
	}


	public boolean getShowButtonIcon() {
		return showButtonIcon;
	}


	public void setShowButtonIcon(boolean showButtonIcon) {
		boolean changed = (this.showButtonIcon != showButtonIcon);

		this.showButtonIcon = showButtonIcon;

		if (changed) {
			changeShowButtonTextIcon();
			requestLayout();
		}

	}


	public boolean getShowButtonText() {
		return showButtonText;
	}


	public void setShowButtonText(boolean showButtonText) {
		boolean changed = (this.showButtonText != showButtonText);

		this.showButtonText = showButtonText;

		if (changed) {
			changeShowButtonTextIcon();
			requestLayout();
		}

	}


	/**
	 * @param buttonId
	 * @return -1 if invalid buttonId, 1 if button visible, 0 if not visible
	 */
	public int getButtonVisible(int buttonId) {
		if (buttonId < 0 || buttonId > countButton) {
			return  -1;
		}

		if (buttons[buttonId].getVisibility() == VISIBLE) {
			return 1;
		} else {
			return 0;
		}

	}


	/**
	 *
	 * @param buttonId
	 * @param visible
	 * @return false if invalid buttonId, true otherwise
	 */
	public boolean setButtonVisible(int buttonId, boolean visible) {
		if (buttonId < 0 || buttonId > countButton) {
			return false;
		}

		if (visible) {
			buttons[buttonId].setVisibility(VISIBLE);
		} else {
			buttons[buttonId].setVisibility(GONE);
		}

		return true;
	}



	@Override
	public void setOrientation(int orientation) {
		super.setOrientation(orientation);

		changeButtonLayoutParams();
		requestLayout();
	}


	private void changeButtonLayoutParams() {
		int paramWidth = 0;
		int paramHeight = 0;

		switch (getOrientation()) {
			case VERTICAL:
				paramWidth = ViewGroup.LayoutParams.MATCH_PARENT;
				break;

			case HORIZONTAL:
				paramHeight = ViewGroup.LayoutParams.MATCH_PARENT;
				break;

		}

		for (int i = 0; i < countButton; ++i) {
			LinearLayout.LayoutParams params =
				(LinearLayout.LayoutParams)buttons[i].getLayoutParams();

			params.width = paramWidth;
			params.height = paramHeight;
		}

	}


	private void changeShowButtonTextIcon() {
		if (showButtonText) {
			for (int i = 0; i < countButton; ++i) {
				buttons[i].setText(titles[i]);
			}

		} else {
			for (int i = 0; i < countButton; ++i) {
				buttons[i].setText(null);
			}

		}

		if (showButtonIcon) {
			for (int i = 0; i < countButton; ++i) {
				this.buttons[i].setCompoundDrawables(
					this.icons[i], null, null, null
				);

			}

		} else {
			for (int i = 0; i < countButton; ++i) {
				this.buttons[i].setCompoundDrawables(
					null, null, null, null
				);

			}

		}

	}


	private Button makeButton() {
		LinearLayout.LayoutParams layoutParams
			= new LinearLayout.LayoutParams(0, 0, 1);

		layoutParams.setMargins(0, 0, 0, 0);

		Button btn = new Button(getContext());
		btn.setMinWidth(1);
		btn.setMinimumWidth(1);
		btn.setMinHeight(1);
		btn.setMinimumHeight(1);

		btn.setLayoutParams(layoutParams);
		btn.setAllCaps(false);

		return btn;
	}






	private class MyOnClickListener implements OnClickListener {

		@Override
		public void onClick(View view) {
			if (clickListener != null) {
				clickListener.onClick(view.getId() - BUTTON_ID_OFFSET);
			}

		}

	}



	public interface OnButtonClickListener {
		void onClick(int index);
	}

}

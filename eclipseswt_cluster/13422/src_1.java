package org.eclipse.swt.examples.controlexample;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved
 */

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.events.*;

class LabelTab extends AlignableTab {

	/* Example widgets and groups that contain them */
	Label label1, label2, label3, label4, label5, label6;
	Group textLabelGroup, imageLabelGroup;

	/* Style widgets added to the "Style" group */
	Button separatorButton, horizontalButton, verticalButton, shadowInButton, shadowOutButton;
/**
* Creates the "Example" group.
*/
void createExampleGroup () {
	super.createExampleGroup ();
	
	/* Create a group for the text labels */
	textLabelGroup = new Group(exampleGroup, SWT.NULL);
	GridLayout gridLayout = new GridLayout ();
	textLabelGroup.setLayout (gridLayout);
	gridLayout.numColumns = 3;
	textLabelGroup.setLayoutData (new GridData (GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
	textLabelGroup.setText (resControls.getString("Text_Labels"));

	/* Create a group for the image labels */
	imageLabelGroup = new Group (exampleGroup, SWT.SHADOW_NONE);
	gridLayout = new GridLayout ();
	imageLabelGroup.setLayout (gridLayout);
	gridLayout.numColumns = 3;
	imageLabelGroup.setLayoutData (new GridData (GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
	imageLabelGroup.setText (resControls.getString("Image_Labels"));
}
/**
* Creates the "Example" widgets.
*/
void createExampleWidgets () {
	
	/* Compute the widget style */
	int style = SWT.NONE;
	if (separatorButton.getSelection ()) style |= SWT.SEPARATOR;
	if (horizontalButton.getSelection ()) style |= SWT.HORIZONTAL;
	if (verticalButton.getSelection ()) style |= SWT.VERTICAL;
	if (shadowInButton.getSelection ()) style |= SWT.SHADOW_IN;
	if (shadowOutButton.getSelection ()) style |= SWT.SHADOW_OUT;
	if (borderButton.getSelection ()) style |= SWT.BORDER;

	/* Create the example widgets */
	label1 = new Label (textLabelGroup, style);
	label1.setText(resControls.getString("One"));
	label2 = new Label (textLabelGroup, style);
	label2.setText(resControls.getString("Two"));
	label3 = new Label (textLabelGroup, style);
	label3.setText (resControls.getString("Three"));
	label4 = new Label (imageLabelGroup, style);
	label4.setImage (Images.CLOSED_FOLDER_IMAGE);
	label5 = new Label (imageLabelGroup, style);
	label5.setImage (Images.OPEN_FOLDER_IMAGE);
	label6 = new Label(imageLabelGroup, style);
	label6.setImage (Images.TARGET_IMAGE);
}
/**
* Creates the "Style" group.
*/
void createStyleGroup() {
	super.createStyleGroup ();
	
	/* Create the extra widgets */
	separatorButton = new Button (styleGroup, SWT.CHECK);
	separatorButton.setText (resControls.getString("SWT_SEPARATOR"));
	horizontalButton = new Button (styleGroup, SWT.RADIO);
	horizontalButton.setText (resControls.getString("SWT_HORIZONTAL"));
	verticalButton = new Button (styleGroup, SWT.RADIO);
	verticalButton.setText (resControls.getString("SWT_VERTICAL"));
	Group styleSubGroup = new Group (styleGroup, SWT.NULL);
	styleSubGroup.setLayout (new GridLayout ());
	shadowInButton = new Button (styleSubGroup, SWT.RADIO);
	shadowInButton.setText (resControls.getString("SWT_SHADOW_IN"));
	shadowOutButton = new Button (styleSubGroup, SWT.RADIO);
	shadowOutButton.setText (resControls.getString("SWT_SHADOW_OUT"));
	borderButton = new Button(styleGroup, SWT.CHECK);
	borderButton.setText(resControls.getString("SWT_BORDER"));

	/* Add the listeners */
	SelectionListener selectionListener = new SelectionAdapter () {
		public void widgetSelected(SelectionEvent event) {
			if ((event.widget.getStyle() & SWT.RADIO) != 0) {
				if (!((Button) event.widget).getSelection ()) return;
			}
			recreateExampleWidgets ();
		};
	};
	shadowInButton.addSelectionListener (selectionListener);
	shadowOutButton.addSelectionListener (selectionListener);
}
/**
* Gets the "Example" widget children.
*/
Control [] getExampleWidgets () {
	return new Control [] {label1, label2, label3, label4, label5, label6};
}
/**
* Gets the text for the tab folder item.
*/
String getTabText () {
	return resControls.getString("Label");
}
/**
* Sets the alignment of the "Example" widgets.
*/
void setExampleWidgetAlignment () {
	int allignment = 0;
	if (leftButton.getSelection ()) allignment = SWT.LEFT;
	if (centerButton.getSelection ()) allignment = SWT.CENTER;
	if (rightButton.getSelection ()) allignment = SWT.RIGHT;
	label1.setAlignment (allignment);
	label2.setAlignment (allignment);
	label3.setAlignment (allignment);
	label4.setAlignment (allignment);
	label5.setAlignment (allignment);
	label6.setAlignment (allignment);
}
/**
* Sets the state of the "Example" widgets.
*/
void setExampleWidgetState () {
	super.setExampleWidgetState ();
	boolean isSeparator = (label1.getStyle () & SWT.SEPARATOR) != 0;
	leftButton.setSelection (!isSeparator && (label1.getStyle () & SWT.LEFT) != 0);
	centerButton.setSelection (!isSeparator && (label1.getStyle () & SWT.CENTER) != 0);
	rightButton.setSelection (!isSeparator && (label1.getStyle () & SWT.RIGHT) != 0);
	shadowInButton.setSelection (isSeparator && (label1.getStyle () & SWT.SHADOW_IN) != 0);
	shadowOutButton.setSelection (isSeparator && (label1.getStyle () & SWT.SHADOW_OUT) != 0);
	leftButton.setEnabled (!isSeparator);
	centerButton.setEnabled (!isSeparator);
	rightButton.setEnabled (!isSeparator);
	shadowInButton.setEnabled (isSeparator);
	shadowOutButton.setEnabled (isSeparator);
	horizontalButton.setEnabled (isSeparator);
	verticalButton.setEnabled (isSeparator);
}
}

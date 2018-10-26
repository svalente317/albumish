/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SyncManagerDialog implements SelectionListener {

    private static final int TEXTBOX_NUM_COLUMNS = 120;
    private static final int TEXTBOX_NUM_LINES = 10;
    private Shell dialog;
    private Text textbox;
    private SyncManager sync_manager;

    public SyncManagerDialog(Shell main_window, String header, String description,
            SyncManager sync_manager) {
        this.sync_manager = sync_manager;
        String newlines = "";
        for (int count = 0; count < TEXTBOX_NUM_COLUMNS; count++) {
            newlines += " ";
        }
        for (int count = 0; count < TEXTBOX_NUM_LINES; count++) {
            newlines += "\n";
        }
        this.dialog = new Shell(main_window, SWT.DIALOG_TRIM | SWT.MODELESS);
        this.dialog.setText("Sync to Device...");

        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.dialog.setLayout(layout);
        GridData data;

        Label top_label = new Label(this.dialog, 0);
        top_label.setText(header);
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        top_label.setLayoutData(data);

        this.textbox = new Text(this.dialog, SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
        this.textbox.setText(newlines);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
        this.textbox.setLayoutData(data);

        Button button = new Button(this.dialog, SWT.PUSH);
        button.setText("OK");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false);
        data.widthHint = 60;
        button.setLayoutData(data);
        button.setData(SWT.OK);
        button.addSelectionListener(this);

        button = new Button(this.dialog, SWT.PUSH);
        button.setText("Cancel");
        button.setLayoutData(data);
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

        this.dialog.pack();
        this.dialog.open();
        this.textbox.setText(description);
        this.textbox.setEditable(false);
        // TODO Give this dialog box keyboard or window manager focus?
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        if (event.widget.getData().equals(SWT.CANCEL)) {
            this.dialog.close();
            return;
        }
        if (event.widget.getData().equals(SWT.OK)) {
            this.dialog.close();
            this.sync_manager.apply_changes_to_device();
            return;
        }
    }
}

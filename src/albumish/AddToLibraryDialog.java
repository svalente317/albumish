/*
 *  Copyright (c) 2014  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.text.SimpleDateFormat;
import java.util.Date;

public class AddToLibraryDialog implements SelectionListener {

    private AddToLibraryManager manager;
    private final Shell dialog;
    private final Text entry;
    private final Text tags_entry;

    public AddToLibraryDialog(AddToLibraryManager manager) {
        this.manager = manager;
        this.dialog = new Shell(manager.jukebox.main_window, SWT.SHELL_TRIM | SWT.MODELESS);
        this.dialog.setText("Add Folder to Library");
        GridLayout layout = new GridLayout(2, false);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.dialog.setLayout(layout);
        GridData data;

        Label label = new Label(this.dialog, 0);
        label.setText("Enter Folder to Add to Library.");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        label.setLayoutData(data);

        this.entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
        data.widthHint = 180;
        this.entry.setLayoutData(data);
        this.entry.setData(SWT.YES);
        this.entry.addSelectionListener(this);

        label = new Label(this.dialog, 0);
        label.setText("Tags:");
        data = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
        label.setLayoutData(data);

        this.tags_entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        this.tags_entry.setData(SWT.YES);
        data = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        this.tags_entry.setLayoutData(data);

        String default_tag = new SimpleDateFormat("yyyyMMdd").format(new Date());
        this.tags_entry.setText(default_tag);

        label = new Label(this.dialog, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Composite button_box = new Composite(this.dialog, 0);
        FillLayout fill_layout = new FillLayout(SWT.HORIZONTAL);
        fill_layout.spacing = 40;
        button_box.setLayout(fill_layout);
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        button_box.setLayoutData(data);

        Button button = new Button(button_box, SWT.PUSH);
        button.setText("OK");
        button.setData(SWT.OK);
        button.addSelectionListener(this);

        button = new Button(button_box, SWT.PUSH);
        button.setText("Cancel");
        button.setData(SWT.CANCEL);
        button.addSelectionListener(this);

        this.dialog.pack();
        this.dialog.open();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
        if ((int) event.widget.getData() == SWT.YES) {
            process_ok_button();
        }
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        switch ((int) event.widget.getData()) {
        case SWT.OK:
            process_ok_button();
            break;
        case SWT.CANCEL:
            this.dialog.close();
            break;
        default:
            break;
        }
    }

    private void process_ok_button() {
        String text = this.entry.getText();
        if (text != null && !text.isEmpty()) {
            String tags = this.tags_entry.getText();
            this.dialog.close();
            this.manager.add_folder_to_library(text, tags);
        }
    }
}

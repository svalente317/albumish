/*
 *  Copyright (c) 2017  Salvatore Valente <svalente@mit.edu>
 *
 *  This program is free software.  You can modify and distribute it under
 *  the terms of the GNU General Public License.  There is no warranty.
 *  See the file "COPYING" for more information.
 */
package albumish;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class ProgressDialog {

    private Shell dialog;
    private ProgressBar progress;
    private Label bottom_label;

    public ProgressDialog(Shell main_window, String title, String description) {
        this.dialog = new Shell(main_window, SWT.DIALOG_TRIM | SWT.MODELESS);
        this.dialog.setText(title);
        RowLayout layout = new RowLayout(SWT.VERTICAL);
        layout.marginWidth = layout.marginHeight = 20;
        layout.spacing = 20;
        layout.fill = true;
        this.dialog.setLayout(layout);
        Label top_label = new Label(this.dialog, 0);
        top_label.setText(description);
        this.progress = new ProgressBar(this.dialog, SWT.HORIZONTAL);
        this.bottom_label = new Label(this.dialog, 0);
        this.dialog.pack();
        this.dialog.open();
    }

    public void set_bottom_label(final String text) {
        this.dialog.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                ProgressDialog pthis = ProgressDialog.this;
                pthis.bottom_label.setText(text);
            }
        });
    }

    public void set_progress(final int current, final int total) {
        this.dialog.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                ProgressDialog pthis = ProgressDialog.this;
                pthis.progress.setMinimum(0);
                pthis.progress.setMaximum(total);
                pthis.progress.setSelection(current);
            }
        });
    }

    public void close_and_run(final Runnable runnable) {
        this.dialog.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                ProgressDialog pthis = ProgressDialog.this;
                pthis.dialog.close();
                if (runnable != null) {
                    runnable.run();
                }
            }
        });
    }
}

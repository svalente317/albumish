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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class InputDialog implements SelectionListener {

    public interface InputRunnable {
        void run(String input);
    }

    private enum Cmd {
        ENTRY,
        OK_BUTTON,
        CANCEL_BUTTON
    }

    private final Shell dialog;
    private final Text entry;
    private final InputRunnable runnable;

    public InputDialog(Shell main_window, String title, String prompt, String defval,
            InputRunnable runnable) {
        this.runnable = runnable;

        this.dialog = new Shell(main_window, SWT.SHELL_TRIM | SWT.MODELESS);
        this.dialog.setText(title);
        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth = layout.marginHeight = 20;
        layout.verticalSpacing = 20;
        this.dialog.setLayout(layout);
        GridData data;

        Label label = new Label(this.dialog, 0);
        label.setText(prompt);
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false, 2, 1);
        label.setLayoutData(data);

        this.entry = new Text(this.dialog, SWT.SINGLE | SWT.BORDER);
        if (defval != null) {
            this.entry.setText(defval);
        }
        data = new GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1);
        data.widthHint = 180;
        this.entry.setLayoutData(data);
        this.entry.setData(Cmd.ENTRY);
        this.entry.addSelectionListener(this);

        label = new Label(this.dialog, SWT.SEPARATOR | SWT.HORIZONTAL);
        data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.horizontalSpan = 2;
        label.setLayoutData(data);

        Button button = new Button(this.dialog, SWT.PUSH);
        button.setText("OK");
        data = new GridData(SWT.CENTER, SWT.BEGINNING, true, false);
        data.widthHint = 60;
        button.setLayoutData(data);
        button.setData(Cmd.OK_BUTTON);
        button.addSelectionListener(this);

        button = new Button(this.dialog, SWT.PUSH);
        button.setText("Cancel");
        button.setLayoutData(data);
        button.setData(Cmd.CANCEL_BUTTON);
        button.addSelectionListener(this);

        this.dialog.pack();
        this.dialog.open();
    }

    @Override
    public void widgetDefaultSelected(SelectionEvent event) {
        if (event.widget.getData() == Cmd.ENTRY) {
            process_ok_button();
        }
    }

    @Override
    public void widgetSelected(SelectionEvent event) {
        switch ((Cmd) event.widget.getData()) {
        case OK_BUTTON:
            process_ok_button();
            break;
        case CANCEL_BUTTON:
            this.dialog.close();
            break;
        default:
            break;
        }
    }

    private void process_ok_button() {
        String text = this.entry.getText();
        if (text != null && !text.isEmpty()) {
            this.dialog.close();
            this.runnable.run(text);
        }
    }
}

package sc.fiji.samj.gui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import io.bioimage.modelrunner.apposed.appose.Mamba;
import io.bioimage.samj.SamEnvManager;
import sc.fiji.samj.communication.model.SAMModel;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.components.GridPanel;
import sc.fiji.samj.gui.components.HTMLPane;

public class SAMModelPanel extends JPanel implements ActionListener {
	
	private static final long serialVersionUID = 7623385356575804931L;

	private HTMLPane info = new HTMLPane(400, 70);
    private int waitingIter = 0;
	
	private JButton bnInstall = new JButton("Install");
	private JButton bnUninstall = new JButton("Uninstall");
	
	private JProgressBar progressInstallation = new JProgressBar();

	private ArrayList<JRadioButton> rbModels = new ArrayList<JRadioButton>();
	private SAMModels models;
	private final SamEnvManager manager;
	
	public SAMModelPanel(SAMModels models) {
		super();
		manager = SamEnvManager.create((str) -> addHtml(str));
		this.models = models;
		JToolBar pnToolbarModel = new JToolBar();
		pnToolbarModel.setFloatable(false);
		pnToolbarModel.setLayout(new GridLayout(1, 2));
		pnToolbarModel.add(bnInstall);
		pnToolbarModel.add(bnUninstall);
		
		ButtonGroup group = new ButtonGroup();
		for(SAMModel model : models) {
			JRadioButton rb = new JRadioButton(model.getName(), model.isInstalled());
			rbModels.add(rb);
			rb.addActionListener(this);
			group.add(rb);
		}
		rbModels.get(0).setSelected(true);
	
		JPanel pnManageModel = new JPanel(new BorderLayout());
		pnManageModel.add(pnToolbarModel, BorderLayout.NORTH);
		pnManageModel.add(progressInstallation, BorderLayout.SOUTH);
		pnManageModel.add(new JScrollPane(info), BorderLayout.CENTER);
		
		GridPanel pnModel = new GridPanel(true);
		int col = 1;
		for(JRadioButton rb : rbModels)
			pnModel.place(1, col++, 1, 1, rb);
		
		pnModel.place(2, 1, 5, 2, pnManageModel);
		
		add(pnModel);
		info.append("p", "Description of the model");
		info.append("p", "Link to source");
		bnInstall.addActionListener(this);
		bnUninstall.addActionListener(this);
		
		updateInterface();
	}
	
	private void updateInterface() {
		for(int i=0; i<rbModels.size(); i++) {
			if (!rbModels.get(i).isSelected()) continue;
			info.clear();
			info.append("p", models.get(i).getDescription());
			bnInstall.setEnabled(!models.get(i).isInstalled());
			bnUninstall.setEnabled(models.get(i).isInstalled());
		}
	}
	
	public SAMModel getSelectedModel() {
		for(int i=0; i<rbModels.size(); i++) {
			if (rbModels.get(i).isSelected())
				return models.get(i);
		}
		return null;
	}
	
	public SamEnvManager getInstallationManager() {
		return this.manager;
	}
	
	public boolean isInstallationEnabled() {
		return this.bnInstall.isEnabled();
	}
	
	private Thread createInstallationThread() {
		Thread installThread = new Thread(() -> {
			try {
				SwingUtilities.invokeLater(() -> installationInProcess(true));
				this.manager.installEfficientSAMSmall();
				SwingUtilities.invokeLater(() -> installationInProcess(false));
				this.progressInstallation.setValue(100);
			} catch (Exception e1) {
				e1.printStackTrace();
				SwingUtilities.invokeLater(() -> installationInProcess(false));
			}
		});
		return installThread;
	}
	
	private Thread createControlThread(Thread installThread) {
		Thread currentThread = Thread.currentThread();
		Thread controlThread = new Thread(() -> {
			while (currentThread.isAlive() && installThread.isAlive()) {
				try{ Thread.sleep(50); } catch(Exception ex) {}
			}
			if (!currentThread.isAlive()) installThread.interrupt();
		});
		return controlThread;
	}
	
	private Thread createInfoThread(Thread installThread) {
		Thread infoThread = new Thread(() -> {
			while (installThread.isAlive()) {
				try{ Thread.sleep(50); } catch(Exception ex) {}
			}
		});
		return infoThread;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == bnInstall) {
			Thread installThread = createInstallationThread();
			installThread.start();
			Thread controlThread = createControlThread(installThread);
			controlThread.start();
		} else if (e.getSource() == bnUninstall) {
			//TODO IJ.log("TODO: call the uninstallation of ");
		}
		
		updateInterface();
	}
	
	private void installationInProcess(boolean install) {
		this.bnUninstall.setEnabled(!install);
		this.bnInstall.setEnabled(!install);
		this.rbModels.stream().forEach(btn -> btn.setEnabled(!install));
		this.progressInstallation.setIndeterminate(install);
	}

    /**
     * Sets the HTML text to be displayed.
     * 
     * @param html
     *        HTML text.
     * @throws NullPointerException
     *         If the HTML text is null.
     */
    public void setHtml(String html) throws NullPointerException
    {
        Objects.requireNonNull(html, "HTML text is null");
        info.setText(formatHTML(html));
        info.setCaretPosition(0);
    }

    /**
     * Sets the HTML text to be displayed and moves the caret until the end of the text
     * 
     * @param html
     *        HTML text.
     * @throws NullPointerException
     *         If the HTML text is null.
     */
    public void setHtmlAndDontMoveCaret(String html) throws NullPointerException {
        Objects.requireNonNull(html, "HTML text is null");
        HTMLDocument doc = (HTMLDocument) info.getDocument();
        HTMLEditorKit editorKit = (HTMLEditorKit) info.getEditorKit();
        try {
            doc.remove(0, doc.getLength());
            editorKit.insertHTML(doc, doc.getLength(), formatHTML(html), 0, 0, null);
        } catch (BadLocationException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return HTML text shown in this component.
     */
    public String getHtml()
    {
        return info.getText();
    }

    public void addHtml(String html) {
        if (html == null) return;
        if (html.trim().isEmpty()) {
        	html = manageEmptyMessage(html);
        } else {
        	waitingIter = 0;
        }
        String nContent = formatHTML(html);
        
        SwingUtilities.invokeLater(() -> {
            try {
                HTMLDocument doc = (HTMLDocument) info.getDocument();
                HTMLEditorKit editorKit = (HTMLEditorKit) info.getEditorKit();
            	editorKit.insertHTML(doc, doc.getLength(), nContent, 0, 0, null);
            	info.setCaretPosition(doc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    public static String formatHTML(String html) {
	    html = html.replace(System.lineSeparator(), "<br>")
	            .replace("    ", "&emsp;")
	            .replace("  ", "&ensp;")
	            .replace(" ", "&nbsp;");
	
	    if (html.startsWith(Mamba.ERR_STREAM_UUUID)) {
	    	html = "<span style=\"color: red;\">" + html.replace(Mamba.ERR_STREAM_UUUID, "") + "</span>";
	    } else {
	    	html = "<span style=\"color: black;\">" + html + "</span>";
	    }
	    return html;
    }
    
    private String manageEmptyMessage(String html) {
    	if (html.trim().isEmpty() && waitingIter == 0) {
        	html = LocalDateTime.now().toString() + " -- Working, this operation migh take several minutes .";
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 1) {
        	html = LocalDateTime.now().toString() + " -- Working, this operation migh take several minutes . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 2) {
        	html = LocalDateTime.now().toString() + " -- Working, this operation migh take several minutes . . .";
        	int len = html.length() - (" .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        } else if (html.trim().isEmpty() && waitingIter % 3 == 0) {
        	html = LocalDateTime.now().toString() + " -- Working, this operation migh take several minutes .";
        	int len = html.length() + (" . .").length() + System.lineSeparator().length();
        	SwingUtilities.invokeLater(() -> {
        		HTMLDocument doc = (HTMLDocument) info.getDocument();
        		try {doc.remove(doc.getLength() - len, len);} catch (BadLocationException e) {}
        	});
        	waitingIter += 1;
        }
    	return html;
    }
}



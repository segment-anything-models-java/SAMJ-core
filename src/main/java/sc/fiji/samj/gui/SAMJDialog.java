package sc.fiji.samj.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import gui.components.GridPanel;
import gui.icons.ButtonIcon;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.gui.ImageCanvas;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.process.ImageProcessor;
import sc.fiji.samj.communication.model.SAMModels;
import sc.fiji.samj.gui.tools.SimulationSAM;
import sc.fiji.samj.gui.tools.Tools;

public class SAMJDialog extends JDialog implements ActionListener, MouseListener {

	private int count = 0;

	private JButton bnClose = new JButton("Close");
	private JButton bnHelp = new JButton("Help");
	private JButton bnStart = new JButton("Start/Encode");
	private JButton bnStop = new JButton("Stop");
	private JButton bnComplete = new JButton("Auto-Complete");
	private JButton bnRoi2Mask = new JButton("Create Mask");
	private JTextField txtStatus = new JTextField("(c) SAMJ team 2024");

	private ImagePlus imp;
	private ImagePlus mask;
	
	private ButtonIcon bnRect = new ButtonIcon("Rect", "rect.png");
	private ButtonIcon bnPoints = new ButtonIcon("Points", "edit.png");
	private ButtonIcon bnBrush = new ButtonIcon("Brush", "github.png");
	private ButtonIcon bnMask = new ButtonIcon("Mask", "help.png");
	private JCheckBox chkROIManager = new JCheckBox("Add to ROI Manager", true);

	private JComboBox<String> cmbImage = new JComboBox<String>();
	
	private SAMModelPanel panelModel;
	private SAMRoiManager roiManager;
	
	private boolean encodingDone = false;
	
	public SAMJDialog(ImagePlus imp, SAMModels models) {
		super(new JFrame(), "SAMJ Annotator");
		this.imp = imp;
		
		ImageCanvas canvas = imp.getCanvas();
		canvas.addMouseListener(this);

		panelModel = new SAMModelPanel(models);
		// Buttons
		JPanel pnButtons = new JPanel(new FlowLayout());
		pnButtons.add(bnRect);
		pnButtons.add(bnPoints);
		pnButtons.add(bnBrush);
		pnButtons.add(bnMask);
		
		// Status
		JToolBar pnStatus = new JToolBar();
		pnStatus.setFloatable(false);
		pnStatus.setLayout(new BorderLayout());
		pnStatus.add(bnHelp, BorderLayout.EAST);
		pnStatus.add(txtStatus, BorderLayout.CENTER);
		pnStatus.add(bnClose, BorderLayout.WEST);

		JPanel pnActions = new JPanel(new FlowLayout());
		pnActions.add(bnRoi2Mask);
		pnActions.add(bnComplete);
		pnActions.add(chkROIManager);
		
		ArrayList<String> listImages = getListImages();
		for(String nameImage : listImages)
			cmbImage.addItem(nameImage);
	
		GridPanel panelImage = new GridPanel(true);
		panelImage.place(1, 1, 1, 1, bnStart);
		panelImage.place(1, 2, 1, 1, cmbImage);
		
		GridPanel pn = new GridPanel();
		pn.place(1, 1, panelModel);
		pn.place(2, 1, panelImage);
		pn.place(3, 1, pnButtons);
		pn.place(4, 1, pnActions);
		
		setLayout(new BorderLayout());
		add(pn, BorderLayout.NORTH);		
		add(pnStatus, BorderLayout.SOUTH);		

		bnRoi2Mask.addActionListener(this);		
		bnComplete.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		chkROIManager.addActionListener(this);
		
		bnStart.addActionListener(this);
		bnStop.addActionListener(this);
		bnRect.addActionListener(this);
		bnPoints.addActionListener(this);
		bnBrush.addActionListener(this);
		bnMask.setDropTarget(new LocalDropTarget());
		
		add(pn);
		pack();
		this.setResizable(false);
		this.setModal(false);
		this.setVisible(true);
		GUI.center(this);
		updateInterface();
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == bnRect) {
			IJ.setTool(Toolbar.RECT_ROI);
		}
		
		if (e.getSource() == bnPoints) {
			IJ.setTool(Toolbar.POINT);
		}
		
		if (e.getSource() == bnBrush) {
			IJ.setTool(Toolbar.FREELINE);
		}
		
		if (e.getSource() == bnHelp) {
			Tools.help();
		}
		
		if (e.getSource() == bnClose) {
			if (imp != null) {
				ImageCanvas canvas = imp.getCanvas();
				if (canvas != null) {
					canvas.removeMouseListener(this);
				}
			}
			roiManager.close();
			dispose();
		}
		
		if (e.getSource() == bnComplete) {
			IJ.log("TO DO call Auto-complete");
		}

		if (e.getSource() == bnStart) {
			IJ.log("TO DO Start the encoding");
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			IJ.log("TO DO End the encoding");
			encodingDone = true;
			roiManager = new SAMRoiManager(imp);
		}
		updateInterface();
	}

	public void updateInterface() {
		bnRect.setEnabled(encodingDone);
		bnPoints.setEnabled(encodingDone);
		bnBrush.setEnabled(encodingDone);
		bnMask.setEnabled(encodingDone);
		
		bnComplete.setEnabled(roiManager.getCount() > 0);
		bnRoi2Mask.setEnabled(roiManager.getCount() > 0);
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		Roi roi = imp.getRoi();
		if (chkROIManager.isSelected())
		if (roi != null) {
			int py = this.getLocation().y + this.getSize().width;
			int px = this.getLocation().x;
			
			Rectangle rect = roi.getBounds();
			IJ.log(rect.toString());
			double chrono = System.nanoTime();
			SimulationSAM sam = new SimulationSAM(imp, rect);
			sam.run();
			PolygonRoi proi = sam.getResultAsRoi();
			chrono = System.nanoTime() - chrono;
			IJ.log(" len: " +  proi.getContainedPoints().length + " score: " + String.format("%3.3f", sam.getResultScore() )  + " time: " + String.format("%3.3f", (chrono / 1000000)) + " ms</small><br>");			//new PolygonRoi(p, PolygonRoi.POLYGON);
			proi.setName("SAM-" + (++count));
			roiManager.add(proi);
			imp.setRoi(proi);
			imp.updateAndRepaintWindow();
			//roiManager.setLocation(new Point(px, py));
		}
		updateInterface();
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
		// Handle mouse enter event
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// Handle mouse exit event
	}


	public class LocalDropTarget extends DropTarget {

		@Override
		public void drop(DropTargetDropEvent e) {
			e.acceptDrop(DnDConstants.ACTION_COPY);
			e.getTransferable().getTransferDataFlavors();
			Transferable transferable = e.getTransferable();
			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			for (DataFlavor flavor : flavors) {
				if (flavor.isFlavorJavaFileListType()) {
					try {
						List<File> files = (List<File>) transferable.getTransferData(flavor);
						for (File file : files) {
							mask = getImageMask(file);
							if (mask != null) {
								return;
							}
						}
					}
					catch (UnsupportedFlavorException ex) {
						ex.printStackTrace();
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				}
			}
			e.dropComplete(true);
			super.drop(e);
		}
	}

	private ImagePlus getImageMask(File file) {
		System.out.println("" + file.getAbsolutePath());
		ImagePlus tmp = IJ.openImage(file.getAbsolutePath());
		if (tmp == null)
			return null;
		/*
		if (imp.getWidth() != tmp.getWidth())
			return null;
		if (imp.getHeight() != tmp.getHeight())
			return null;
			*/
		ImageProcessor ip = tmp.getProcessor();
		mask = new ImagePlus(file.getName(), ip);
		mask.show();
		IJ.log(mask.toString());
		return mask;
	}
	
	private ArrayList<String> getListImages() {
		int[] ids = WindowManager.getIDList();
		ArrayList<String> list = new ArrayList<String>();
		if (ids != null) {
			for (int id : ids) {
				ImagePlus idp = WindowManager.getImage(id);
				if (idp != null) {
					list.add((String)idp.getTitle());
				}
			}
		}
		return list;
	}

}

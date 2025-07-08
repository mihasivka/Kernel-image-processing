import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class MainWindow extends JFrame {

	public static JFrame mainWindow;
	public static String imagePath;
	public static float fac;
	public static float[][] kernelMatrix;
	private JFileChooser FileChooser;
	public static BufferedImage processedImage;

	private String Path1;

	private JMenuItem kernel1;
	private JMenuItem kernel2;
	private JMenuItem kernel3;
	private JMenuItem kernel4;
	// private int setKernel;
	private JButton applyButton;
	private JLabel img1Text;
	private JLabel img2Text;
	private JLabel img1;
	private JLabel img2;
	private JRadioButton sequentialButton;
	private JRadioButton parallelButton;
	private JRadioButton distributedButton;
	private ButtonGroup processingGroup;

	public MainWindow() {

		mainWindow = this;

		this.setSize(1200, 800);
		this.setTitle("Kernel image processing");
		this.setLocationRelativeTo(null);

		FileChooser = new JFileChooser();
		FileChooser.setCurrentDirectory(new File("src/Images"));
		FileChooser.setDialogTitle("Open file");
		FileChooser.setPreferredSize(new Dimension(400, 300));

		JMenuBar MenuBar = new JMenuBar();
		JMenu Menu = new JMenu("Menu");
		JMenu subMenu = new JMenu("Kernels");
		JMenuItem select = new JMenuItem("Select File");
		select.addActionListener(e -> selectImage(e));

		kernel1 = new JMenuItem("Edge detection");
		kernel2 = new JMenuItem("Sharpen");
		kernel3 = new JMenuItem("Blur");
		kernel4 = new JMenuItem("Emboss");

		kernel1.addActionListener(e -> setKernel1());
		kernel2.addActionListener(e -> setKernel2());
		kernel3.addActionListener(e -> setKernel3());
		kernel4.addActionListener(e -> setKernel4());

		MenuBar.add(Menu);
		subMenu.add(kernel1);
		subMenu.add(kernel2);
		subMenu.add(kernel3);
		subMenu.add(kernel4);
		Menu.add(select);
		Menu.add(subMenu);
		this.setJMenuBar(MenuBar);

		JPanel panel = new JPanel();
		img1Text = new JLabel("Before: ");
		img1 = new JLabel();
		img1.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		img1.setPreferredSize(new Dimension(300, 300));

		img2Text = new JLabel("After: ");
		img2 = new JLabel();
		img2.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		img2.setPreferredSize(new Dimension(300, 300));

		applyButton = new JButton("Apply");
		applyButton.addActionListener(e -> {
			try {
				if (kernelMatrix == null) {
					setKernel1();
				}
				if (sequentialButton.isSelected()) {
					Sequential.convolution(Path1, kernelMatrix, fac);
					insertImg("src/Temp/temp.jpg", img2);
				} else if (parallelButton.isSelected()) {
					parallel.convolution(Path1, kernelMatrix, fac);
					insertImg("src/Temp/temp.jpg", img2);
				} else if (distributedButton.isSelected()) {
					JOptionPane.showMessageDialog(this, "Not implemented yet.", "Info",
							JOptionPane.INFORMATION_MESSAGE);
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		});
		panel.add(img1Text);
		panel.add(img1);
		panel.add(img2Text);
		panel.add(img2);
		panel.add(applyButton);
		this.add(panel);

		sequentialButton = new JRadioButton("Sequential", true);
		parallelButton = new JRadioButton("Parallel");
		distributedButton = new JRadioButton("Distributed");

		processingGroup = new ButtonGroup();

		processingGroup.add(sequentialButton);
		processingGroup.add(parallelButton);
		processingGroup.add(distributedButton);

		JPanel radioPanel = new JPanel();

		radioPanel.add(sequentialButton);
		radioPanel.add(parallelButton);
		radioPanel.add(distributedButton);

		panel.setLayout(new BorderLayout());
		panel.add(radioPanel, BorderLayout.NORTH);

		JPanel imagesPanel = new JPanel();

		imagesPanel.add(img1Text);
		imagesPanel.add(img1);
		imagesPanel.add(img2Text);
		imagesPanel.add(img2);

		panel.add(imagesPanel, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(applyButton);
		panel.add(buttonPanel, BorderLayout.SOUTH);
		this.add(panel);

		this.setVisible(true);
		this.setResizable(false);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

	}

	public static void insertImg(String Path, JLabel label) {
		ImageIcon icon = new ImageIcon(new ImageIcon(Path).getImage().getScaledInstance(label.getWidth(),
				label.getHeight(), Image.SCALE_SMOOTH));
		label.setIcon(icon);
	}

	private void selectImage(ActionEvent e) {
		FileChooser.addChoosableFileFilter(
				new FileNameExtensionFilter("Images (.jpg, .jpeg, .png)", "jpg", "jpeg", "png"));

		int returnVal = FileChooser.showOpenDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = FileChooser.getSelectedFile();
			Path1 = file.getAbsolutePath();
			insertImg(Path1, img1);
		} else {
			System.out.println("File access cancelled.");
		}
	}

	private void setKernel1() {
		kernelMatrix = Kernels.edge_detection;

		fac = 1;
	}

	private void setKernel2() {
		kernelMatrix = Kernels.sharpen;

		fac = 1;
	}

	private void setKernel3() {
		kernelMatrix = Kernels.blur;

		fac = 1f / 9f;
	}

	private void setKernel4() {
		kernelMatrix = Kernels.emboss;

		fac = 1;
	}

}

package tools.refinery.generator.gui;

import com.google.inject.Inject;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.generator.standalone.StandaloneRefinery;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;

import com.github.weisj.jsvg.SVGDocument;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Simple Swing GUI that shows a list of items and a button. When the button
 * is pressed (or an item is double-clicked) the selected item is passed to
 * {@link #onItemSelected(Object)} which is left as a stub for the user to
 * implement.
 */
public class RefineryGui {
	@Inject
	private static PropagationAdapter propagationAdapter;
	private static DesignSpaceExplorationAdapter designSpaceExplorationAdapter;
	private static ReasoningAdapter reasoningAdapter;
	private static ProblemTrace trace;
	private final JFrame frame;
	private final DefaultListModel<Activation> listModel;
	private final JList<Activation> list;
	private final JPanel imagePanel;
	private boolean autoFit = true;
	private double zoomFactor = 1.0;
	private SVGDocument currentSvg;
	// optional handler set by the caller; if non-null it's invoked instead of onItemSelected
	private Consumer<Activation> selectionHandler;
	// optional supplier to refresh the list contents after each send action
	private Supplier<List<Activation>> itemsSupplier;
	private static Model model;
	private static ModelQueryAdapter queryEngine;
	private static int id = 0;

	public RefineryGui() {
		frame = new JFrame("Refinery GUI");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null);

		listModel = new DefaultListModel<>();
		list = new JList<>(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane listScrollPane = new JScrollPane(list);
		listScrollPane.setPreferredSize(new Dimension(300, 0));

		imagePanel = new JPanel() {
			@Override
			public Dimension getPreferredSize() {
				if (currentSvg != null && !autoFit) {
					com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
					return new Dimension((int) (size.width * zoomFactor), (int) (size.height * zoomFactor));
				}
				return super.getPreferredSize();
			}

			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				if (currentSvg != null) {
					com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
					float iw = size.width;
					float ih = size.height;
					if (iw > 0 && ih > 0) {
						double scale;
						int x = 0, y = 0;
						if (autoFit) {
							Container parent = getParent();
							int w = parent != null ? parent.getWidth() : getWidth();
							int h = parent != null ? parent.getHeight() : getHeight();
							scale = Math.min((double) w / iw, (double) h / ih);
							x = (int) (w - iw * scale) / 2;
							y = (int) (h - ih * scale) / 2;
						} else {
							scale = zoomFactor;
						}
						Graphics2D g2d = (Graphics2D) g.create();
						g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
						g2d.translate(x, y);
						g2d.scale(scale, scale);
						currentSvg.render(null, g2d);
						g2d.dispose();
					}
				}
			}
		};

		JScrollPane imageScrollPane = new JScrollPane(imagePanel);
		imageScrollPane.setWheelScrollingEnabled(false);

		imagePanel.addMouseWheelListener(e -> {
			if (currentSvg == null) return;

			Point mousePoint = e.getPoint();
			double oldZoom = zoomFactor;

			if (autoFit) {
				autoFit = false;
				com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
				Container parent = imagePanel.getParent();
				int w = parent != null ? parent.getWidth() : imagePanel.getWidth();
				int h = parent != null ? parent.getHeight() : imagePanel.getHeight();
				zoomFactor = Math.min((double) w / size.width, (double) h / size.height);
				oldZoom = zoomFactor;
			}

			if (e.getWheelRotation() < 0) {
				zoomFactor *= 1.1;
			} else {
				zoomFactor /= 1.1;
			}

			// Adjust the scroll position so that the mouse point stays fixed
			JViewport viewport = imageScrollPane.getViewport();
			Point viewPosition = viewport.getViewPosition();

			double scaleChange = zoomFactor / oldZoom;
			int newX = (int)((viewPosition.x + mousePoint.x) * scaleChange - mousePoint.x);
			int newY = (int)((viewPosition.y + mousePoint.y) * scaleChange - mousePoint.y);

			imagePanel.revalidate();
			imagePanel.repaint();

			SwingUtilities.invokeLater(() -> {
				viewport.setViewPosition(new Point(Math.max(0, newX), Math.max(0, newY)));
			});
		});

		MouseAdapter dragAdapter = new MouseAdapter() {
			private Point origin;

			@Override
			public void mousePressed(MouseEvent e) {
				origin = e.getPoint();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (origin != null && !autoFit) {
					JViewport viewPort = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, imagePanel);
					if (viewPort != null) {
						int deltaX = origin.x - e.getX();
						int deltaY = origin.y - e.getY();
						Rectangle view = viewPort.getViewRect();
						view.x += deltaX;
						view.y += deltaY;
						imagePanel.scrollRectToVisible(view);
					}
				}
			}
		};
		imagePanel.addMouseListener(dragAdapter);
		imagePanel.addMouseMotionListener(dragAdapter);

		JPanel rightPanel = new JPanel(new BorderLayout());
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton zoomInBtn = new JButton("+");
		JButton zoomOutBtn = new JButton("-");
		JButton fitBtn = new JButton("⛶");

		zoomInBtn.addActionListener(e -> {
			if (autoFit && currentSvg != null) {
				autoFit = false;
				com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
				Container parent = imagePanel.getParent();
				int w = parent != null ? parent.getWidth() : imagePanel.getWidth();
				int h = parent != null ? parent.getHeight() : imagePanel.getHeight();
				zoomFactor = Math.min((double) w / size.width, (double) h / size.height);
			}
			zoomFactor *= 1.2;
			imagePanel.revalidate();
			imagePanel.repaint();
		});

		zoomOutBtn.addActionListener(e -> {
			if (autoFit && currentSvg != null) {
				autoFit = false;
				com.github.weisj.jsvg.geometry.size.FloatSize size = currentSvg.size();
				Container parent = imagePanel.getParent();
				int w = parent != null ? parent.getWidth() : imagePanel.getWidth();
				int h = parent != null ? parent.getHeight() : imagePanel.getHeight();
				zoomFactor = Math.min((double) w / size.width, (double) h / size.height);
			}
			zoomFactor /= 1.2;
			imagePanel.revalidate();
			imagePanel.repaint();
		});

		fitBtn.addActionListener(e -> {
			autoFit = true;
			imagePanel.revalidate();
			imagePanel.repaint();
		});

		toolbar.add(zoomInBtn);
		toolbar.add(zoomOutBtn);
		toolbar.add(fitBtn);

		rightPanel.add(toolbar, BorderLayout.NORTH);
		rightPanel.add(imageScrollPane, BorderLayout.CENTER);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listScrollPane, rightPanel);
		splitPane.setDividerLocation(300);

		JButton sendButton = new JButton("Send Selected");
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendSelected();
			}
		});

		// double-click to send
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					sendSelected();
				}
			}
		});

		JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		bottom.add(sendButton);

		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(splitPane, BorderLayout.CENTER);
		frame.getContentPane().add(bottom, BorderLayout.SOUTH);
	}

	public void setSvg(SVGDocument svg) {
		this.currentSvg = svg;
		if (imagePanel != null) {
			imagePanel.revalidate();
			imagePanel.repaint();
		}
	}

	private void sendSelected() {
		Activation sel = list.getSelectedValue();
		if (sel == null) {
			JOptionPane.showMessageDialog(frame, "No item selected", "Warning", JOptionPane.WARNING_MESSAGE);
			return;
		}
		if (selectionHandler != null) {
			selectionHandler.accept(sel);
		} else {
			onItemSelected(sel);
		}
		// after every press of the button, refresh the list from the supplier if provided
		refreshItems();
	}

	private void refreshItems() {
		if (itemsSupplier != null) {
			try {
				List<Activation> items = itemsSupplier.get();
				if (items != null) {
					setItems(items);
				}
			} catch (Exception ex) {
				// don't crash the UI if the supplier throws; show a warning
				JOptionPane.showMessageDialog(frame, "Error while refreshing items: " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	/**
	 * Set a handler that will be invoked when an item is sent.
	 * If not set, {@link #onItemSelected(Object)} is called instead.
	 */
	public void setSelectionHandler(Consumer<Activation> handler) {
		this.selectionHandler = handler;
	}

	/**
	 * Set a supplier which will be invoked after every send/button press to
	 * refresh the list contents. The supplier should return a List of items
	 * to display (may return null to leave the list unchanged).
	 */
	public void setItemsSupplier(Supplier<List<Activation>> supplier) {
		this.itemsSupplier = supplier;
	}

	/**
	 * Stub to be implemented by the user. This method will be called when an
	 * item is sent (button click or double-click). Replace the body with the
	 * desired behavior (e.g. call into StandaloneRefinery or other code).
	 */
	protected void onItemSelected(Object item) {
		// TODO: implement behavior when an item is selected. Example:
		// StandaloneRefinery.process(item);
		System.out.println("Selected: " + item);
	}

	/** Replace the list contents with the given items. */
	public void setItems(List<Activation> items) {
		listModel.clear();
		for (Activation o : items) {
			listModel.addElement(o);
		}
	}

	public void show() {
		frame.setVisible(true);
	}

	private static void refreshActivations(List<Activation> activationList) {
		activationList.clear();
		for (var transformation : designSpaceExplorationAdapter.getTransformations()) {
			System.out.println(transformation.getDefinition().rule().getName());
			System.out.println(transformation.getAllActivationsAsResultSet().size());
			var activationCursor = transformation.getAllActivationsAsResultSet().getAll();
			while (activationCursor.move()) {
				activationList.add(new Activation(transformation, activationCursor.getKey()));
			}
		}
	}

	private static void showModel(ModelGenerator generator, RefineryGui gui) {
		SVGDocument svg = Visualizer.renderModel(model);
		if (gui != null) {
			gui.setSvg(svg);
		}
		var stateProblem = generator.serialize();
		var resource = stateProblem.eResource();
		try {
			resource.save(System.out, Map.of());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void step(Activation activation) {
		System.out.println(activation.fire());
		System.out.println(propagationAdapter.propagate());
		System.out.println(designSpaceExplorationAdapter.checkAccept());
		model.commit();
		queryEngine.flushChanges();
	}


	public static void main(String[] args) throws IOException {
		var problem = StandaloneRefinery.getProblemLoader().loadString("""
				% Metamodel
				class Person {
				    contains Post[] posts opposite author
				    contains Picture[] pictures
				    Person[] friend opposite friend
				    Dog[] pets opposite owner
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				class Picture.

				class Dog {
					Person owner opposite pets
				}

				% Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				% Instance model
				!friend(a, b).
				author(p1, a).
				author(p2, b).

				!author(Post::new, a).

				% Scope
				scope Post = 5, Person = 5.
				""");
		var generator =
				StandaloneRefinery.getGeneratorFactory().debugPartialInterpretations(true).createGenerator(problem);
		model = generator.getModel();
		var initial = model.commit();
		model.restore(initial);
		queryEngine = model.getAdapter(ModelQueryAdapter.class);
		trace = generator.getProblemTrace();
		propagationAdapter = model.getAdapter(PropagationAdapter.class);
		designSpaceExplorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {


				RefineryGui gui = new RefineryGui();
				// Demo: mutable list that will be updated when the user sends an item.
				final List<Activation> activationList = new ArrayList<>();
				showModel(generator, gui);
				refreshActivations(activationList);

				// supply the current contents of activationList whenever the GUI refreshes
				gui.setItemsSupplier(() -> new ArrayList<>(activationList));

				// when an item is selected, remove it from activationList to simulate processing
				gui.setSelectionHandler(item -> {
					System.out.println("Processing: " + item);
					step(item);
					showModel(generator, gui);
					refreshActivations(activationList);
				});

				// initialize GUI list from the supplier
				gui.refreshItems();

				gui.show();

			}
		});
	}
}

package de.csbd.learnathon.command;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;


import net.imagej.table.*;
import net.imglib2.histogram.Histogram1d;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.command.CommandModule;
import org.scijava.command.CommandService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import org.scijava.ui.behaviour.util.Behaviours;

import bdv.viewer.state.ViewerState;
import circledetection.command.BlobDetectionCommand;
import ij.ImagePlus;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Pair;
import net.imglib2.view.Views;

public class PunctaPickerController {

    public static String ACTION_NONE = "none";
    public static String ACTION_TRACK = "track";
    public static String ACTION_SELECT = "select";
    private String actionIndicator = ACTION_NONE;

    private RealPoint pos;
    private PunctaPickerModel model;
    private PunctaPickerView view;
    private CommandService cs;
    private ThreadService ts;
    private OpService os;

    public PunctaPickerController(PunctaPickerModel model, PunctaPickerView punctaPickerView, CommandService cs, ThreadService ts, OpService os) {
        this.model = model;
        this.view = punctaPickerView;
        this.cs = cs;
        this.ts = ts;
        this.os = os;
    }

    public void defineBehaviour() {
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_A, 0), "Add", new ManualTrackingAction("Add"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_E, 0), "IncreaseRadius", new ManualTrackingAction("IncreaseRadius"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_Q, 0), "DecreaseRadius", new ManualTrackingAction("DecreaseRadius"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_C, 0), "Select", new ManualTrackingAction("Select"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "Move", new ManualTrackingAction("Move"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "Link", new ManualTrackingAction("Link"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_X, 0), "DeleteTracklet", new ManualTrackingAction("DeleteTracklet"));
        registerKeyBinding(KeyStroke.getKeyStroke(KeyEvent.VK_D, 0), "DeletePunctaOrEdge", new ManualTrackingAction("DeletePunctaOrEdge"));
    }

    public void registerKeyBinding(KeyStroke keyStroke, String name, Action action) {
        InputMap im = view.getBdv().getViewerPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = view.getBdv().getViewerPanel().getActionMap();

        im.put(keyStroke, name);
        am.put(name, action);
    }

    public class ManualTrackingAction extends AbstractAction {
        private String name;

        public ManualTrackingAction(String name) {
            this.name = name;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Behaviours behaviours = new Behaviours(new InputTriggerConfig());
            behaviours.install(view.getBdv().getBdvHandle().getTriggerbindings(), "my-new-behaviours");
            if (name == "Add") {
                actionIndicator = "track";
                behaviours.behaviour((ClickBehaviour) (x, y) -> {
                    clickAction(x, y);
                }, "Add", "A");
            }
            if (name == "IncreaseRadius") {
                Puncta pun = model.getGraph().getLeadSelectedPuncta();
                if (!(pun == null)) {
                    pun.setR(pun.getR() * 1.2f);
                    model.getView().getBdv().getViewerPanel().requestRepaint();
                }
            }
            if (name == "DecreaseRadius") {
                if (!(model.getGraph().getLeadSelectedPuncta() == null)) {
                    model.getGraph().getLeadSelectedPuncta().setR(model.getGraph().getLeadSelectedPuncta().getR() * 0.8f);
                    model.getView().getBdv().getViewerPanel().requestRepaint();
                }
            }
            if (name == "Select") {
                actionIndicator = "select";
                behaviours.behaviour((ClickBehaviour) (x, y) -> {
                    actionSelectClosestSubgraph(x, y);
                }, "Select", "C");
            }
            if (name == "Move") {
                behaviours.behaviour((ClickBehaviour) (x, y) -> {
                    actionMoveLeadPuncta(x, y);
                }, "Move", "SPACE");
                model.getView().getBdv().getViewerPanel().requestRepaint();
            }
            if (name == "Link") {
                if (!(model.getGraph().getLeadSelectedPuncta() == null) && !(model.getGraph().getMouseSelectedPuncta() == null)) {
                    model.getGraph().addEdge(new Edge(model.getGraph().getLeadSelectedPuncta(), model.getGraph().getMouseSelectedPuncta()));
                    model.getGraph().selectSubgraphContaining(model.getGraph().getLeadSelectedPuncta());
                    model.getView().getBdv().getViewerPanel().requestRepaint();
                }
            }
            if (name == "DeleteTracklet") {
                model.getGraph().deleteSelectedElements();
                model.getView().getBdv().getViewerPanel().requestRepaint();
            }
            if (name == "DeletePunctaOrEdge") {
                if (!(model.getGraph().getMouseSelectedEdge() == null)) {
                    model.getGraph().removeEdge(model.getGraph().getMouseSelectedEdge());
                    model.getGraph().setMouseSelectedEdge(null);
                    model.getGraph().selectSubgraphContaining(model.getGraph().getLeadSelectedPuncta()); //Trial Basis
                    model.getView().getBdv().getViewerPanel().requestRepaint();
                } else {
                    model.getGraph().deleteSelectedPuncta();
                    model.getView().getBdv().getViewerPanel().requestRepaint();

                }
            }
        }
    }

    private void clickAction(int x, int y) { // TODO clickAction and actionClick might not be self expainatory... ;)
        if (actionIndicator.equals(ACTION_SELECT)) {
            actionSelectClosestSubgraph(x, y);
        } else if (actionIndicator.equals(ACTION_TRACK)) {
            actionClick(x, y);
        }
    }

    /**
     * Sets one of the ACTION_* strings statically defined in this class.
     *
     * @param s any of the ACTION_* strings defined in this class
     */
    public void setActionIndicator(String s) {
        actionIndicator = s;
    }

    public String getActionIndicator() {
        return actionIndicator;
    }

    private void actionSelectClosestSubgraph(int x, int y) {
        Graph g = model.getGraph();

        if (!g.getPunctas().isEmpty()) {
            view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);

            Pair<Puncta, Double> minEval = PPGraphUtils.getClosestPuncta(pos.getFloatPosition(0), pos.getFloatPosition(1), g.getPunctas());
            if (minEval != null) {
                Puncta minDistPuncta = minEval.getA();
                double minDist = Math.sqrt(minEval.getB());
                if (minDistPuncta.getR() >= minDist) {
                    minDistPuncta.setSelected(true);
                    g.setLeadSelectedPuncta(minDistPuncta);
                    g.selectSubgraphContaining(minDistPuncta);
                    view.getBdv().getViewerPanel().setTimepoint(minDistPuncta.getT());
                }
                view.getBdv().getViewerPanel().requestRepaint();
            }
        }
    }


    private <T extends RealType<T> & NativeType<T>> void actionClick(int x, int y) {
        ts.run(() -> {
            actionClickInThread(x, y);
        });
    }

    private <T extends RealType<T> & NativeType<T>> void actionClickInThread(int x, int y) {

        pos = new RealPoint(3);
        view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);
        ViewerState state = view.getBdv().getBdvHandle().getViewerPanel().getState();
        int t = state.getCurrentTimepoint();

        Graph g = model.getGraph();
        Puncta pOld = g.getLeadSelectedPuncta();


        if (!g.getPunctaAtTime(t).isEmpty()) {
            Pair<Puncta, Double> min = PPGraphUtils.getClosestPuncta(pos.getFloatPosition(0), pos.getFloatPosition(1), g.getPunctaAtTime(t));

            if (min.getB() < min.getA().getR()) {
                addSelectedEdge(g, pOld, min.getA());
                model.getGraph().setLeadSelectedPuncta(min.getA());
            }
        }

        Img<T> image = view.getImage();
        Views.extendMirrorSingle(image);
        int patchSize = 50;
        FinalInterval cropped = Intervals.createMinMax((long) (pos.getDoublePosition(0) - patchSize / 2), (long) (pos.getDoublePosition(1) - patchSize / 2), 0, (long) (pos.getDoublePosition(0) + patchSize / 2), (long) (pos.getDoublePosition(1) + patchSize / 2), 0);
        RandomAccessibleInterval<T> croppedImage = Views.interval(image, cropped);
        ImagePlus imgPlus = ImageJFunctions.wrap(croppedImage, "cropped");
//		ImageJFunctions.show(croppedImage);
        Img<T> newImage = ImageJFunctions.wrap(imgPlus);

        Puncta pNew = detectFeatures(newImage);
        pNew.setT(t);
        pNew.setX((float) (pos.getDoublePosition(0) - patchSize / 2) + pNew.getX());
        pNew.setY((float) (pos.getDoublePosition(1) - patchSize / 2) + pNew.getY());

        model.getGraph().addPuncta(pNew);
        model.getGraph().setLeadSelectedPuncta(pNew);
        pNew.setSelected(true);
        if (pOld != null && pOld.getT() == t - 1) {
            addSelectedEdge(g, pOld, pNew);
        } else {
            model.getGraph().unselectAll();
            pNew.setSelected(true);
        }
        view.getBdv().getViewerPanel().nextTimePoint();
    }

    private void addSelectedEdge(Graph g, Puncta p1, Puncta p2) {
        Edge newE = new Edge(p1, p2);
        p1.setSelected(true);
        p2.setSelected(true);
        g.addEdge(newE);
        newE.setSelected(true);
    }

    public void actionMoveLeadPuncta(int x, int y) {
        view.getBdv().getBdvHandle().getViewerPanel().displayToGlobalCoordinates(x, y, pos);
        Puncta lsp = model.getGraph().getLeadSelectedPuncta();
        lsp.setX(pos.getFloatPosition(0));
        lsp.setY(pos.getFloatPosition(1));
    }

    private <T extends RealType<T> & NativeType<T>> Puncta detectFeatures(Img<T> image) {
        double minScale = 2;
        double stepScale = 1;
        double maxScale = 5;
        boolean brightBlobs = true;
        int axis = 0;
        double samplingFactor = 1;

        final Future<CommandModule> lp = cs.run(BlobDetectionCommand.class, false, "image", image, "minScale", minScale, "maxScale", maxScale, "stepScale", stepScale, "brightBlobs", brightBlobs, "axis", axis, "samplingFactor", samplingFactor);
        final GenericTable resultsTable = (GenericTable) cs.moduleService().waitFor(lp).getOutput("resultsTable");
        Column valueColumn = resultsTable.get("Value");
        Iterator<Float> valueIterator = valueColumn.iterator();

        /*Step Two: Find Otsu Threshold Value on the new List, so obtained*/
        SampleList<FloatType> localMinimaResponse = createIterableList(resultsTable.get("Value"));
        Histogram1d<FloatType> hist = os.image().histogram(localMinimaResponse);
        float otsuThreshold = (float) os.threshold().otsu(hist).getRealFloat();
        //GenericTable thresholdedTable = getThresholdedLocalMinima(otsuThreshold, resultsTable);

        GenericTable thresholdedLocalMinimaTable=new DefaultGenericTable();
        Column valueOld = resultsTable.get("Value");
        Column XOld = resultsTable.get("X");
        Column YOld = resultsTable.get("Y");
        Column radiusOld = resultsTable.get("Radius");

        Iterator valueOldIterator = valueOld.iterator();
        Iterator XOldIterator = XOld.iterator();
        Iterator YOldIterator = YOld.iterator();
        Iterator radiusOldIterator = radiusOld.iterator();


        FloatColumn XColumn = new FloatColumn("X");
        FloatColumn YColumn = new FloatColumn("Y");
        IntColumn RadiusColumn = new IntColumn("Radius");
//		FloatColumn ValueColumn = new FloatColumn("Value");

        int counter=0;
        while (valueOldIterator.hasNext()) {

            if ((float) valueOldIterator.next() <= otsuThreshold) {
                XColumn.add((float) XOldIterator.next());
                YColumn.add((float) YOldIterator.next());
                RadiusColumn.add((int) radiusOldIterator.next());

            } else {
                XOldIterator.next();
                YOldIterator.next();
                radiusOldIterator.next();
            }
        counter++;
        }
        thresholdedLocalMinimaTable.add(XColumn);
        thresholdedLocalMinimaTable.add(YColumn);
        thresholdedLocalMinimaTable.add(RadiusColumn);




        double min;
        int index = 0;
        int i = 0;
        double c1 = valueIterator.next();

        while (valueIterator.hasNext()) {

            double c2 = valueIterator.next();
            if (c1 <= c2) {
                min = c1;
                index = i;
            } else {
                min = c2;
                index = i + 1;
            }
            c1 = min;
            i++;
        }

        Column X = resultsTable.get("X");
        int x = (int) X.get(index);
        Column Y = resultsTable.get("Y");
        int y = (int) Y.get(index);
        Column Radius = resultsTable.get("Radius");
        float r = (float) Radius.get(index);
        Puncta p = new Puncta(x, y, 0, (int) r); // Fix time later on, depending on the frame
        return p;
    }

    private SampleList<FloatType> createIterableList(final Column column) {

        final Iterator<Float> iterator = column.iterator();
        final List<FloatType> imageResponse = new ArrayList<>();
        while (iterator.hasNext()) {
            imageResponse.add(new FloatType(iterator.next().floatValue()));
        }

        return new SampleList<>(imageResponse);
    }

    private GenericTable getThresholdedLocalMinima(float threshold, GenericTable resultsTable) {
        GenericTable thresholdedLocalMinimaTable = new DefaultGenericTable();
        Column valueOld = resultsTable.get("Value");
        Column XOld = resultsTable.get("X");
        Column YOld = resultsTable.get("Y");
        Column radiusOld = resultsTable.get("Radius");

        Iterator valueOldIterator = valueOld.iterator();
        Iterator XOldIterator = XOld.iterator();
        Iterator YOldIterator = YOld.iterator();
        Iterator radiusOldIterator = radiusOld.iterator();


        FloatColumn XColumn = new FloatColumn("X");
        FloatColumn YColumn = new FloatColumn("Y");
        IntColumn RadiusColumn = new IntColumn("Radius");
//		FloatColumn ValueColumn = new FloatColumn("Value");


        while (valueOldIterator.hasNext()) {

            if ((float) valueOldIterator.next() <= threshold) {
                XColumn.add((float) XOldIterator.next());
                YColumn.add((float) YOldIterator.next());
                RadiusColumn.add((int) radiusOldIterator.next());

            } else {
                XOldIterator.next();
                YOldIterator.next();
                radiusOldIterator.next();
            }

        }
        thresholdedLocalMinimaTable.add(XColumn);
        thresholdedLocalMinimaTable.add(YColumn);
        thresholdedLocalMinimaTable.add(RadiusColumn);

        return thresholdedLocalMinimaTable;


    }


}

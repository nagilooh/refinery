package tools.refinery.generator.gui;

import com.github.weisj.jsvg.SVGDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class RefineryGuiModel {
    private List<Activation> activations = new ArrayList<>();
    private SVGDocument currentSvg;

    public interface Listener {
        void onActivationsChanged(List<Activation> activations);
        void onSvgChanged(SVGDocument svg);
    }

    private final List<Listener> listeners = new ArrayList<>();

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public void setActivations(List<Activation> activations) {
        this.activations = new ArrayList<>(activations);
        for (Listener l : listeners) {
            l.onActivationsChanged(this.activations);
        }
    }

    public List<Activation> getActivations() {
        return activations;
    }

    public void setSvg(SVGDocument svg) {
        this.currentSvg = svg;
        for (Listener l : listeners) {
            l.onSvgChanged(this.currentSvg);
        }
    }

    public SVGDocument getSvg() {
        return currentSvg;
    }
}


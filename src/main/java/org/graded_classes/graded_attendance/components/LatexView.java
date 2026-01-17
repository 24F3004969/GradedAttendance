package org.graded_classes.graded_attendance.components;

import javafx.beans.DefaultProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.StringPropertyBase;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableFloatProperty;
import javafx.css.StyleableProperty;
import javafx.css.converter.SizeConverter;
import javafx.geometry.NodeOrientation;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.Font;
import org.jfree.fx.FXGraphics2D;
import org.scilab.forge.jlatexmath.ParseException;
import org.scilab.forge.jlatexmath.TeXConstants;
import org.scilab.forge.jlatexmath.TeXFormula;
import org.scilab.forge.jlatexmath.TeXIcon;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * The {@code LatexView} is a JavaFX {@code Node} used for rendering LaTeX formulas
 *
 * @author Egor Makarenko
 */
@DefaultProperty("formula")
public class LatexView extends Canvas {

    /**
     * Font size to use by default.
     */
    private static final float DEFAULT_SIZE = (float) Font.getDefault().getSize();

    /**
     * LaTeX formula to use by default.
     */
    private static final String DEFAULT_FORMULA = "";

    static {
        // Where the fonts live relative to the classpath root:
        String[] roots = {
                "org/scilab/forge/jlatexmath/fonts/",
                "org/scilab/forge/jlatexmath/cyrillic/fonts/",
                "org/scilab/forge/jlatexmath/greek/fonts/"
        };

        for (String root : roots) {
            URL url = LatexView.class.getClassLoader().getResource(root);
            if (url == null) {
                System.err.println("No resource directory on classpath: " + root);
                continue;
            }
            try {
                if ("jar".equals(url.getProtocol())) {
                    String spec = url.toString();
                    int bang = spec.indexOf("!/");
                    URI jarUri = URI.create(spec.substring(0, bang));
                    String entry = spec.substring(bang + 1);
                    try (FileSystem fs = FileSystems.newFileSystem(jarUri, Map.of())) {
                        Path dir = fs.getPath("/" + entry);
                        try (Stream<Path> s = Files.walk(dir)) {
                            s.filter(p -> p.toString().endsWith(".ttf"))
                                    .forEach(p -> {
                                        try (InputStream in = Files.newInputStream(p)) {
                                            Font f = Font.loadFont(in, -1);
                                            if (f == null) System.err.println("Failed to load font: " + p);
                                        } catch (Exception ex) {
                                            System.err.println("Error reading font " + p + " : " + ex);
                                        }
                                    });
                        }
                    }
                } else if ("file".equals(url.getProtocol())) {
                    Path dir = Paths.get(url.toURI());
                    try (Stream<Path> s = Files.walk(dir)) {
                        s.filter(p -> p.toString().endsWith(".ttf"))
                                .forEach(p -> {
                                    try (InputStream in = Files.newInputStream(p)) {
                                        Font f = Font.loadFont(in, -1);
                                        if (f == null) System.err.println("Failed to load font: " + p);
                                    } catch (Exception ex) {
                                        System.err.println("Error reading font " + p + " : " + ex);
                                    }
                                });
                    }
                } else {
                    System.err.println("Unsupported protocol: " + url);
                }
            } catch (Exception e) {
                System.err.println("Failed to load fonts from " + root + " : " + e);
            }
        }
    }

    private TeXIcon texIcon;

    /**
     * Allocates a new {@code LatexView} object.
     */
    public LatexView() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
    }

    /**
     * Allocates a new {@code LatexView} object using the given LaTeX formula.
     *
     * @param formula LaTeX formula that this LatexView uses
     */
    public LatexView(String formula) {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        formulaProperty().set(formula);
    }

    private void update() throws ParseException {
        TeXFormula teXFormula;
        try {
            teXFormula = new TeXFormula(getFormula());
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            teXFormula = new TeXFormula("\\textcolor{red}{\\text{Error}}");
        }

        texIcon = teXFormula.createTeXIcon(TeXConstants.STYLE_DISPLAY, getSize());

        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, getWidth(), getHeight());

        double width = texIcon.getIconWidth();
        double height = texIcon.getIconHeight();

        setWidth(width);
        setHeight(height);

        gc.clearRect(0, 0, width, height);

        FXGraphics2D graphics = new FXGraphics2D(gc);
        texIcon.paintIcon(null, graphics, 0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isResizable() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double prefWidth(double height) {
        return texIcon.getIconWidth();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double prefHeight(double width) {
        return texIcon.getIconHeight();
    }

    /**
     * Formula text
     *
     * @return formula text as a {@code StringProperty}
     */
    public final StringProperty formulaProperty() {
        if (formula == null) {
            formula = new StringPropertyBase(DEFAULT_FORMULA) {
                @Override
                public void invalidated() {
                    LatexView.this.update();
                }

                @Override
                public Object getBean() {
                    return LatexView.this;
                }

                @Override
                public String getName() {
                    return "formula";
                }
            };
        }
        return formula;
    }

    private StringProperty formula;

    public final void setFormula(String value) {
        formulaProperty().set(value == null ? DEFAULT_FORMULA : value);
    }

    public final String getFormula() {
        String value = formulaProperty().get();
        return value == null ? DEFAULT_FORMULA : value;
    }

    /**
     * Formula text size
     *
     * @return formula text size as a {@code FloatProperty}
     */
    public final StyleableFloatProperty sizeProperty() {
        if (size == null) {
            size = new StyleableFloatProperty(DEFAULT_SIZE) {
                @Override
                public void invalidated() {
                    LatexView.this.update();
                }

                @Override
                public Object getBean() {
                    return LatexView.this;
                }

                @Override
                public String getName() {
                    return "size";
                }

                @Override
                public CssMetaData<LatexView, Number> getCssMetaData() {
                    return LatexView.StyleableProperties.SIZE;
                }
            };
        }
        return size;
    }

    private StyleableFloatProperty size;

    public final void setSize(float value) {
        sizeProperty().set(value);
    }

    public final float getSize() {
        return sizeProperty().get();
    }

    /***************************************************************************
     * Stylesheet Handling
     **************************************************************************/

    private static final String DEFAULT_STYLE_CLASS = "latex-view";

    /**
     * Super-lazy instantiation pattern from Bill Pugh.
     *
     * @treat AsPrivate implementation detail
     */
    private static class StyleableProperties {
        private static final CssMetaData<LatexView, Number> SIZE =
                new CssMetaData<LatexView, Number>("-fx-font-size", SizeConverter.getInstance(), DEFAULT_SIZE) {

                    @Override
                    public boolean isSettable(LatexView node) {
                        return node.size == null || !node.size.isBound();
                    }

                    @Override
                    public StyleableProperty<Number> getStyleableProperty(LatexView node) {
                        return node.sizeProperty();
                    }
                };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables =
                    new ArrayList<>();
            styleables.add(SIZE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }

    /**
     * @return The CssMetaData associated with this class, which may include the
     * CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }
}
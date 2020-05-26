import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.contents.DocumentContent;
import com.intellij.diff.tools.holders.TextEditorHolder;
import com.intellij.diff.tools.util.FoldingModelSupport;
import com.intellij.diff.util.DiffDividerDrawUtil;
import com.intellij.diff.util.DiffDrawUtil;
import com.intellij.diff.util.DiffUtil;
import com.intellij.diff.util.TextDiffTypeFactory;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.LineMarkerRenderer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Divider;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.panels.Wrapper;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import refdiffIdea.core.cst.CstNode;
import refdiffIdea.core.cst.Location;
import refdiffIdea.core.diff.CstDiff;
import refdiffIdea.core.diff.Relationship;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RefactoringView implements Disposable {
    final private Relationship relationship;
    final private Project myProject;
    final private CustomFoldingModel folding;
    final CustomSplitter contentSplitter;
    final private List<DocumentContent> documents = new ArrayList<>();
    final private List<TextEditorHolder> panels = new ArrayList<>();
    static private DiffContentFactory contentFactory = DiffContentFactory.getInstance();
    final private Wrapper leftWrapper;
    final private Wrapper rightWrapper;

    // Lines of the text in editor
    int[] oldTextLines = new int[2];
    int[] sameTextLines = new int[2];
    int[] newTextLines = null;

    RefactoringView(@NotNull Project project, @NotNull Relationship relationship,
                    @NotNull CstDiff diff) {
        myProject = project;
        this.relationship = relationship;
        leftWrapper = createEditor(relationship.getNodeBefore());
        EditorEx leftEditor = panels.get(0).getEditor();
        leftEditor.setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_LEFT);
        rightWrapper = createEditor(relationship.getNodeAfter());
        EditorEx rightEditor = panels.get(1).getEditor();
        rightEditor.setVerticalScrollbarOrientation(EditorEx.VERTICAL_SCROLLBAR_RIGHT);

        calculateLinesForNode(relationship.getNodeBefore(), oldTextLines);
        calculateLinesForNode(relationship.getNodeAfter(),  sameTextLines);

        CstNode correspondingNode = null;
        // Trying to find pair of nodes, one of which is
        for (Relationship r : diff.getRelationships()) {
            if (r.getType().isMatching() &&
                    r.getNodeBefore() == relationship.getNodeBefore() &&
                    r.getNodeAfter() != relationship.getNodeAfter()) {
                newTextLines = sameTextLines;
                sameTextLines = new int[2];
                correspondingNode = r.getNodeAfter();
                calculateLinesForNode(r.getNodeAfter(), sameTextLines);
                break;
            }
        }

        RangeHighlighter highlighter = leftEditor.getFilteredDocumentMarkupModel().addRangeHighlighter(
                relationship.getNodeBefore().getLocation().getBegin(),
                relationship.getNodeBefore().getLocation().getEnd(), 5600,
                TextDiffTypeFactory.MODIFIED.getAttributes(leftEditor),
                HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineMarkerRenderer(new CustomLineMarkerRenderer(JBColor.BLUE));

        if (correspondingNode != null) {
            rightEditor.getFilteredDocumentMarkupModel().addRangeHighlighter(
                    relationship.getNodeAfter().getLocation().getBegin(),
                    relationship.getNodeAfter().getLocation().getEnd(), 5600,
                    TextDiffTypeFactory.INSERTED.getAttributes(rightEditor),
                    HighlighterTargetArea.LINES_IN_RANGE);
        } else {
            correspondingNode = relationship.getNodeAfter();
        }
        highlighter = rightEditor.getFilteredDocumentMarkupModel().addRangeHighlighter(
                correspondingNode.getLocation().getBegin(),
                correspondingNode.getLocation().getEnd(), 5600,
                TextDiffTypeFactory.MODIFIED.getAttributes(rightEditor),
                HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineMarkerRenderer(new CustomLineMarkerRenderer(JBColor.BLUE));

        EditorEx[] editors = {leftEditor, rightEditor};
        folding = new CustomFoldingModel(myProject, editors, this);
        for (int i = 0; i < 2; ++i) {
            DiffUtil.installLineConvertor(editors[i], documents.get(i), folding, i);
        }
        folding.install(folding.computeRanges(oldTextLines, sameTextLines, newTextLines), null, folding.getSettings());

        contentSplitter = new CustomSplitter();
        contentSplitter.setDividerWidth(25);
        contentSplitter.setFirstComponent(leftWrapper);
        contentSplitter.setSecondComponent(rightWrapper);
    }

    private void calculateLinesForNode(@NotNull final CstNode node, @NotNull int[] lines) {
        lines[0] = node.getLocation().getLine();
        lines[1] = Location.findLineNumber(node.getLocation().getEnd(), node.getFile().getText());
    }

    private Wrapper createEditor(@NotNull CstNode node) {
        PsiFile file = node.getFile();
        DocumentContent content = contentFactory.create(myProject, file.getViewProvider().getDocument());
        documents.add(content);
        EditorEx editor = DiffUtil.createEditor(content.getDocument(), myProject, true, true);
        DiffUtil.configureEditor(editor, content, myProject);
        TextEditorHolder holder = new TextEditorHolder(myProject, editor);
        panels.add(holder);

        MigLayout mgr = new MigLayout(new LC().flowY().fill().hideMode(3)
                .insets("0").gridGapY("0"));
        Wrapper wrapper = new Wrapper(mgr);
        wrapper.add(DiffUtil.createTitle(node.getFile().getName()),
                new CC().growX().minWidth("0").gapY("0", String.valueOf(DiffUtil.TITLE_GAP)));
        wrapper.add(holder.getComponent(), new CC().grow().push());
        return wrapper;
    }

    public JComponent getSplitter() {
        return contentSplitter;
    }

    @Override
    public void dispose() {
        panels.forEach(Disposer::dispose);
    }

    private static class CustomFoldingModel extends FoldingModelSupport {
        private final MyPaintable paintable = new MyPaintable(0,1);

        public CustomFoldingModel(@Nullable Project project, @NotNull EditorEx[] editors, @NotNull Disposable disposable) {
            super(project, editors, disposable);
        }

        public Data computeRanges(int[] oldLines, int[] sameLines, int[] newLines) {
            List<int[]> ranges = new ArrayList<>();

            ranges.add(new int[]{oldLines[0] - 1, oldLines[1], sameLines[0] - 1, sameLines[1]});
            if (newLines != null) {
                if (newLines[0] > sameLines[0]) { // Added text is lower in editor
                    --ranges.get(0)[1];
                    ranges.add(new int[]{oldLines[1], oldLines[1], newLines[0] - 1, newLines[1]});
                } else { // Added text is higher in editor
                    ++ranges.get(0)[0];
                    ranges.add(0, new int[]{oldLines[0] - 1, oldLines[0], newLines[0] - 1, newLines[1]});
                }
            }

            return computeFoldedRanges(ranges.iterator(), getSettings());
        }

        public void paintOnDivider(@NotNull Graphics2D gg, @NotNull Component divider) {
            paintable.paintOnDivider(gg, divider);
        }

        public FoldingModelSupport.Settings getSettings() {
            return new FoldingModelSupport.Settings(0,false);
        }
    }

    private class CustomSplitter extends Splitter {
        @Override
        protected Divider createDivider() {
            return new DividerImpl() {
                @Override
                protected void paintComponent(@NotNull Graphics g) {
                    super.paintComponent(g);
                    if (panels.isEmpty())
                        return;

                    JComponent component = panels.get(0).getEditor().getComponent();
                    Graphics2D graphics = (Graphics2D)g.create(
                            0, component.getLocationOnScreen().y - getLocationOnScreen().y,
                            getWidth(), component.getHeight());

                    Color color = panels.get(0).getEditor().getColorsScheme().getColor(EditorColors.GUTTER_BACKGROUND);
                    if (color == null)
                        color = EditorColors.GUTTER_BACKGROUND.getDefaultColor();
                    graphics.setColor(color);
                    graphics.fill(graphics.getClipBounds());

                    color = JBColor.BLUE;
                    EditorEx left = panels.get(0).getEditor();
                    EditorEx right = panels.get(1).getEditor();

                    int topOffset1 = left.getHeaderComponent() == null ? 0 : left.getHeaderComponent().getHeight();
                    topOffset1 -= left.getScrollingModel().getVerticalScrollOffset();
                    int topOffset2 = right.getHeaderComponent() == null ? 0 : right.getHeaderComponent().getHeight();
                    topOffset2 -= right.getScrollingModel().getVerticalScrollOffset();
                    DiffDrawUtil.MarkerRange range1 =
                            DiffDrawUtil.getGutterMarkerPaintRange(left, oldTextLines[0] - 1, oldTextLines[1]);
                    DiffDrawUtil.MarkerRange range2 =
                            DiffDrawUtil.getGutterMarkerPaintRange(right, sameTextLines[0] - 1, sameTextLines[1]);

                    DiffDividerDrawUtil.DividerPolygon polygon =
                            new DiffDividerDrawUtil.DividerPolygon(range1.y1 + topOffset1, range2.y1 + topOffset2,
                                    range1.y2 + topOffset1, range2.y2 + topOffset2, color, color, false);
                    polygon.paint(graphics, getWidth(),false);

                    folding.paintOnDivider(graphics, this);
                    graphics.dispose();
                }
            };
        }
    }


    private static class CustomLineMarkerRenderer implements LineMarkerRenderer{
        final JBColor color;
        CustomLineMarkerRenderer(@NotNull JBColor color) {
            this.color = color;
        }

        @Override
        public void paint(Editor editor, Graphics g, Rectangle r) {
            EditorGutterComponentEx gutter = ((EditorEx)editor).getGutterComponentEx();
            g.setColor(color);
            g.fillRect(-5, r.y, gutter.getWidth(), r.height);
        }
    }
}

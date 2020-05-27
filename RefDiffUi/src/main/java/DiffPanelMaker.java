import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.core.diff.CstDiff;
import refdiffIdea.core.diff.CstRootHelper;
import refdiffIdea.core.diff.Relationship;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class DiffPanelMaker implements Disposable {
    final Project myProject;
    final Wrapper content;
    final JPanel diffPanel;

    Consumer<String> changeTitleCallback = null;
    final List<Relationship> relationships;
    final List<CstDiff> cstDiffs;
    int index = 0;
    Map<Relationship, RefactoringView> viewForRelationship = new HashMap<>();

    public DiffPanelMaker(@NotNull final Project project, @NotNull final List<CstDiff> diffs) {
        myProject = project;
        cstDiffs = diffs;
        content = new Wrapper();
        relationships = new ArrayList<>();
        for (CstDiff diff : diffs) {
            relationships.addAll(diff.getRefactoringRelationships());
        }

        JPanel mainPanel = new JPanel(new BorderLayout());
        updateFiles();

        mainPanel.add(content, BorderLayout.CENTER);

        DefaultActionGroup toolbarActionGroup = buildToolbar();
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
                ActionPlaces.TOOLBAR, toolbarActionGroup, true);
        toolbar.setTargetComponent(mainPanel);
        mainPanel.add(toolbar.getComponent(), BorderLayout.NORTH);

        diffPanel = JBUI.Panels.simplePanel(mainPanel);
    }

    public void setTitleChangeCallback(@NotNull final Consumer<String> changeTitleCallback) {
        this.changeTitleCallback = changeTitleCallback;
        changeTitle();
    }

    private DefaultActionGroup buildToolbar() {
        DefaultActionGroup result = new DefaultActionGroup(
                new PrevRefactoringAction(), new NextRefactoringAction());
        return result;
    }

    private void updateFiles() {
        Relationship r = relationships.get(index);
        ListIterator<CstDiff> cstDiffIt = cstDiffs.listIterator();
        for (int count = 0; count < index + 1; ) {
            count += cstDiffIt.next().getRefactoringRelationships().size();
        }

        RefactoringView view = viewForRelationship.computeIfAbsent(r,
                relationship -> new RefactoringView(myProject,relationship, cstDiffIt.previous()));
        content.removeAll();
        content.add(view.getSplitter());
        content.repaint();
        if (changeTitleCallback != null)
            changeTitle();
    }

    private void changeTitle() {
        Relationship r = relationships.get(index);
        String fullNameBefore = String.join(" ", CstRootHelper.getNodePath(r.getNodeBefore()));
        String fullNameAfter = String.join(" ", CstRootHelper.getNodePath(r.getNodeAfter()));
        String title = null;
        switch (r.getType()) {
            case MOVE:
                title = fullNameBefore + " moved to " + fullNameAfter;
                break;
            case RENAME:
                title = fullNameBefore + " renamed to " +  fullNameAfter;
                break;
            case MOVE_RENAME:
                title = fullNameBefore + " moved and renamed to " + fullNameAfter;
                break;
            case EXTRACT:
                title = "Extracted " + fullNameAfter + " from " + fullNameBefore;
                break;
            case CHANGE_SIGNATURE:
                title = "Signature changed for " + fullNameAfter;
                break;
            case EXTRACT_MOVE:
                title = "Extracted and moved " + fullNameAfter + " from " + fullNameBefore;
                break;
            default:
                break;
        }
        changeTitleCallback.accept(title);
    }

    public JPanel getDiffPanel() {
        return diffPanel;
    }

    @Override
    public void dispose() {
        viewForRelationship.values().forEach(Disposer::dispose);
    }

    protected class PrevRefactoringAction extends AnAction {
        PrevRefactoringAction() {
            super("Previous", "Switch to previous refactoring", AllIcons.Vcs.Arrow_left);
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(true);
            if (index == 0) {
                e.getPresentation().setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            --index;
            updateFiles();
        }
    }

    protected class NextRefactoringAction extends AnAction {
        NextRefactoringAction() {
            super("Next", "Switch to next refactoring", AllIcons.Vcs.Arrow_right);
        }
        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(true);
            if (index + 1 == relationships.size()) {
                e.getPresentation().setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ++index;
            updateFiles();
        }
    }
}

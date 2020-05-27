import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.WindowWrapper;
import com.intellij.openapi.ui.WindowWrapperBuilder;
import com.intellij.openapi.util.Disposer;
import com.intellij.vcs.log.VcsFullCommitDetails;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;
import git4idea.GitCommit;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.core.RefDiff;
import refdiffIdea.core.diff.CstDiff;
import refdiffIdea.core.io.GitHelper;

import java.util.ArrayList;
import java.util.List;

public class ShowRefactoringsForCommit extends DumbAwareAction {
    public ShowRefactoringsForCommit() {
        super("Show Refactorings Performed",
                "Tries to form a list of refactorings performed for commit",
                null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final VcsLog log = e.getRequiredData(VcsLogDataKeys.VCS_LOG);
        final VcsFullCommitDetails details = log.getSelectedDetails().get(0);
        GitCommit commit = (GitCommit) details;
        List<CstDiff> diffs = new ArrayList<>();
        for (LanguagePluginCreator creator : LanguagePluginCreator.Extensions.getExtensionList()) {
            RefDiff refDiff = new RefDiff(creator.create(project));
            final CstDiff diff = refDiff.computeDiffForCommit(project, commit);
            if (!diff.getRefactoringRelationships().isEmpty())
                diffs.add(diff);
        }
        showRefactorings(project, diffs);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(true);

        // No plugins, no buttons
        if (LanguagePluginCreator.Extensions.getExtensionList().isEmpty()) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        final Project project = e.getProject();
        if (project == null) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        final VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
        if (log == null || log.getSelectedDetails().size() != 1) {
            e.getPresentation().setEnabledAndVisible(false);
            return;
        }

        if (GitHelper.openRepository(project) != null) {
            e.getPresentation().setEnabledAndVisible(true);
        }
    }

    private void showRefactorings(@NotNull final Project project, @NotNull final List<CstDiff> diffs) {
        if (diffs.isEmpty()) {
            Messages.showInfoMessage("No refactorings found", "RefDiff");
            return;
        }

        DiffPanelMaker maker = new DiffPanelMaker(project, diffs);

        WindowWrapper wrapper = new WindowWrapperBuilder(WindowWrapper.Mode.FRAME, maker.getDiffPanel())
                .setProject(project)
                .setDimensionServiceKey("RefDiffDialog")
                .build();
        Disposer.register(wrapper, maker);

        wrapper.show();
        maker.setTitleChangeCallback(wrapper::setTitle);
    }
}

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
import refdiffIdea.core.diff.Relationship;
import refdiffIdea.core.io.GitHelper;
import refdiffIdea.parsers.java.JavaPlugin;

import java.util.Set;

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
        RefDiff refDiff = new RefDiff(JavaPlugin.create(project));
        final CstDiff cstDiff = refDiff.computeDiffForCommit(project, commit);
        showRefactorings(project, cstDiff);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        e.getPresentation().setVisible(true);

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

    private void showRefactorings(final Project project, final CstDiff diff) {
        Set<Relationship> relationships = diff.getRefactoringRelationships();
        if (relationships.isEmpty()) {
            Messages.showInfoMessage("No refactorings found", "RefDiff");
            return;
        }

        DiffPanelMaker maker = new DiffPanelMaker(project, diff);

        WindowWrapper wrapper = new WindowWrapperBuilder(WindowWrapper.Mode.FRAME, maker.getDiffPanel())
                .setProject(project)
                .setDimensionServiceKey("RefDiffDialog")
                .build();
        Disposer.register(wrapper, maker);

        wrapper.show();
        maker.setTitleChangeCallback(wrapper::setTitle);
    }
}

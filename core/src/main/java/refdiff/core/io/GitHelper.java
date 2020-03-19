package refdiff.core.io;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import refdiff.core.util.PairBeforeAfter;

import java.nio.file.Paths;
import java.util.*;

public class GitHelper {

	public static GitRepository openRepository(Project project) {
		GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
		return manager.getRepositories().stream().filter(x -> x.getProject() == project).findAny().orElse(null);
	}

	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(GitRepository repo, String commitSha1, FilePathFilter filter) throws VcsException {
		try {
			GitCommit commit = GitHistoryUtils.history(repo.getProject(), repo.getRoot(), commitSha1 + " -n1").get(0);
			Collection<Change> changes = commit.getChanges();
			List<SourceFile> filesBefore = new ArrayList<>();
			Map<SourceFile, ContentRevision> mapBefore = new HashMap<>();
			List<SourceFile> filesAfter  = new ArrayList<>();
			Map<SourceFile, ContentRevision> mapAfter = new HashMap<>();
			for (Change change : changes) {
				fillRevisionInfo(change.getBeforeRevision(), filesBefore, mapBefore, filter);
				fillRevisionInfo(change.getAfterRevision(),filesAfter,mapAfter,filter);
			}
			return new PairBeforeAfter<>(new GitSourceTree(repo, commitSha1, filesBefore, mapBefore),
										 new GitSourceTree(repo, commitSha1, filesAfter, mapAfter));
		} catch (VcsException e) {
			throw e;
		}
	}

	public static void fillRevisionInfo(ContentRevision revision, List<SourceFile> files,
										Map<SourceFile, ContentRevision> map, FilePathFilter filter) {
		if (revision != null) {
			String path = revision.getFile().getPath();
			if (filter.isAllowed(path)) {
				SourceFile current = new SourceFile(Paths.get(path));
				files.add(current);
				map.put(current, revision);
			}
		}
	}
	
}

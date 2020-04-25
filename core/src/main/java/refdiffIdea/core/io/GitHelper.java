package refdiffIdea.core.io;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ContentRevision;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import refdiffIdea.core.util.PairBeforeAfter;

import java.nio.file.Paths;
import java.util.*;

public class GitHelper {
	/**
	 * @param project Project which commits should be analysed
	 * @return GitRepository related to the project
	 */
	public static GitRepository openRepository(Project project) {
		GitRepositoryManager manager = GitRepositoryManager.getInstance(project);
		return manager.getRepositories().stream().filter(x -> x.getProject() == project).findAny().orElse(null);
	}

	/**
	 * Fetches two sets of files for a single commit. One set consists of files which were modified or added,
	 * and the other of modified and deleted files.
	 *
	 * @param repo       git repository for current analysis
	 * @param commitSha1 hash of commit to analyze
	 * @param filter	 filter of files based on extensions
	 * @return pair of sets before and after commit
	 * @throws VcsException if there is a problem with running git
	 */
	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(
			final GitRepository repo, String commitSha1, FilePathFilter filter) throws VcsException {
		GitCommit commit = GitHistoryUtils.history(repo.getProject(), repo.getRoot(), commitSha1, "-1").get(0);
		return getSourcesBeforeAndAfterCommit(repo, commit, commitSha1,filter);
	}

	/**
	 * Fetches two sets of files for a single commit. One set consists of files which were modified or added,
	 * and the other of modified and deleted files.
	 *
	 * @param repo   git repository for current analysis
	 * @param commit commit to analyze
	 * @param filter filter of files based on extensions
	 * @return pair of sets before and after commit
	 */
	public static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(
			final GitRepository repo, GitCommit commit, FilePathFilter filter) {
		String commitSha1 = commit.getId().toString();
		return getSourcesBeforeAndAfterCommit(repo, commit, commitSha1,filter);
	}

	/**
	 * Helper for different versions of getSourcesBeforeAndAfterCommit
	 *
	 * @param repo   	 git repository for current analysis
	 * @param commit 	 commit to analyze
	 * @param commitSha1 hash of commit to analyze
	 * @param filter 	 filter of files based on extensions
	 * @return pair of sets before and after commit
	 */
	private static PairBeforeAfter<SourceFileSet> getSourcesBeforeAndAfterCommit(
			final GitRepository repo, final GitCommit commit, final String commitSha1, final FilePathFilter filter) {
		Collection<Change> changes = commit.getChanges();
		List<SourceFile> filesBefore = new ArrayList<>();
		Map<SourceFile, ContentRevision> mapBefore = new HashMap<>();
		List<SourceFile> filesAfter  = new ArrayList<>();
		Map<SourceFile, ContentRevision> mapAfter = new HashMap<>();
		for (Change change : changes) {
			fillRevisionInfo(change.getBeforeRevision(), filesBefore, mapBefore, filter);
			fillRevisionInfo(change.getAfterRevision(), filesAfter, mapAfter, filter);
		}
		return new PairBeforeAfter<>(new GitSourceTree(repo, commitSha1, filesBefore, mapBefore),
				new GitSourceTree(repo, commitSha1, filesAfter, mapAfter));
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

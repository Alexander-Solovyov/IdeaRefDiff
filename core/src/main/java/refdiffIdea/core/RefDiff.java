package refdiffIdea.core;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.vcs.VcsException;
import git4idea.repo.GitRepository;

import refdiffIdea.core.diff.CstComparator;
import refdiffIdea.core.diff.CstDiff;
import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.core.io.GitHelper;
import refdiffIdea.core.io.SourceFileSet;
import refdiffIdea.core.util.PairBeforeAfter;
import refdiffIdea.parsers.LanguagePlugin;

/**
 * High level API of RefDiff, providing methods to compute CST diffs between revisions (commits) of a git repository.
 */
public class RefDiff {
	
	private final CstComparator comparator;
	private final FilePathFilter fileFilter;
	
	/**
	 * Build a RefDiff instance with the specified language plugin. E.g.: {@code new RefDiff(new JsParser())}.
	 * 
	 * @param parser A language parser
	 */
	public RefDiff(LanguagePlugin parser) {
		this.comparator = new CstComparator(parser);
		this.fileFilter = parser.getAllowedFilesFilter();
	}

	/**
	 * Compute CST diff for commit
	 *
	 * @param project 	 context project
	 * @param commitSha1 commit hash
	 * @return			 computed CST Diff
	 * @throws VcsException if no commit for given hash was found
	 */
	public CstDiff computeDiffForCommit(final Project project, String commitSha1) throws VcsException {
		try {
			GitRepository repo = GitHelper.openRepository(project);
			PairBeforeAfter<SourceFileSet> beforeAndAfter = GitHelper.getSourcesBeforeAndAfterCommit(repo, commitSha1, fileFilter);
			return comparator.compare(beforeAndAfter);
		} catch (VcsException e) {
			throw e;
		}
	}

}

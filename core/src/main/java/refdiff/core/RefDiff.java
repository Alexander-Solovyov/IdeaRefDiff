package refdiff.core;

import java.io.File;
import java.util.function.BiConsumer;

import com.intellij.openapi.project.Project;

import com.intellij.openapi.vcs.VcsException;
import git4idea.repo.GitRepository;

import refdiff.core.diff.CstComparator;
import refdiff.core.diff.CstDiff;
import refdiff.core.io.FilePathFilter;
import refdiff.core.io.GitHelper;
import refdiff.core.io.SourceFileSet;
import refdiff.core.util.PairBeforeAfter;
import refdiff.parsers.LanguagePlugin;

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

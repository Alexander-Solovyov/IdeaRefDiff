package refdiffIdea.parsers;

import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.core.io.SourceFileSet;
import refdiffIdea.core.cst.CstRoot;

public interface LanguagePlugin {
	
	CstRoot parse(SourceFileSet sources) throws Exception;
	
	FilePathFilter getAllowedFilesFilter();
	
}

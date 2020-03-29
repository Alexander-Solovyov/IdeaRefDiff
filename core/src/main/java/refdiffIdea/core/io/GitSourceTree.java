package refdiffIdea.core.io;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ContentRevision;
import git4idea.repo.GitRepository;

public class GitSourceTree extends SourceFileSet {

	private final Map<SourceFile, ContentRevision> fileToContent;
	private final GitRepository repository;
	private final String sha1;

	public GitSourceTree(final GitRepository repo, final String sha,
						 final List<SourceFile> sourceFiles, final Map<SourceFile, ContentRevision> fToC) {
		super(sourceFiles);
		fileToContent = fToC;
		repository = repo;
		sha1 = sha;
	}

	@Override
	public String readContent(SourceFile sourceFile) throws IOException {
		try {
			return fileToContent.get(sourceFile).getContent();
		} catch (VcsException e) {
			throw new IOException(e);
		}
	}
	
//	private byte[] readBytes(SourceFile sourceFile) throws MissingObjectException, IncorrectObjectTypeException, IOException, CorruptObjectException, UnsupportedEncodingException, FileNotFoundException {
//		try (ObjectReader reader = repo.newObjectReader(); RevWalk walk = new RevWalk(reader)) {
//			RevCommit commit = walk.parseCommit(sha1);
//
//			RevTree tree = commit.getTree();
//			TreeWalk treewalk = TreeWalk.forPath(reader, sourceFile.getPath(), tree);
//
//			if (treewalk != null) {
//				return reader.open(treewalk.getObjectId(0)).getBytes();
//			} else {
//				throw new FileNotFoundException(sourceFile.getPath());
//			}
//		}
//	}
	
	@Override
	public String describeLocation(SourceFile sourceFile) {
		return String.format("%s:%s:%s", repository.getRoot().getPath(), sha1.substring(0, 7), sourceFile.getPath());
	}
	
	@Override
	public void materializeAt(Path folderPath) throws IOException {
//		File folder = folderPath.toFile();
//		if (folder.exists() || folder.mkdirs()) {
//			for (SourceFile sf : getSourceFiles()) {
//				File destinationFile = new File(folder, sf.getPath());
//				if (!destinationFile.exists()) {
//					byte[] content = readBytes(sf);
//					Files.createDirectories(destinationFile.getParentFile().toPath());
//					Files.write(destinationFile.toPath(), content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
//				}
//			}
//			checkoutFolder = folderPath;
//		} else {
//			throw new IOException("Failed to create directory " + folderPath);
//		}
	}
	
	@Override
	public void materializeAtBase(Path baseFolderPath) throws IOException {
//		Path folder = baseFolderPath.resolve(repo.getDirectory().getName() + "-" + sha1.abbreviate(7).name());
//		materializeAt(folder);
	}
	
	@Override
	public Optional<Path> getBasePath() {
		return Optional.ofNullable(null);
	}
}

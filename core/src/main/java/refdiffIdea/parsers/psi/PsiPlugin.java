package refdiffIdea.parsers.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.tree.TokenSet;

import refdiffIdea.core.cst.CstRoot;
import refdiffIdea.core.cst.TokenPosition;
import refdiffIdea.core.cst.TokenizedSource;
import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.core.io.SourceFile;
import refdiffIdea.core.io.SourceFileSet;
import refdiffIdea.parsers.LanguagePlugin;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PsiPlugin implements LanguagePlugin {
    protected final PsiFileFactory fileFactory;
    protected final LanguageVisitor visitor;
    private Pattern pattern;

    public PsiPlugin(Project project, LanguageVisitor visitor) {
        this.visitor = visitor;
        fileFactory = PsiFileFactory.getInstance(project);
        pattern = Pattern.compile(getPattern());
    }

    @Override
    public CstRoot parse(SourceFileSet sources) throws Exception {
        CstRoot root = new CstRoot();
        visitor.preProcess(root);

        for (SourceFile sourceFile : sources.getSourceFiles()) {
            String path = sourceFile.getPath();
            FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(path);
            String content = sources.readContent(sourceFile);
            PsiFile file = fileFactory.createFileFromText(path, fileType, content);

            List<TokenPosition> tokens = new ArrayList<>();
            tokenize(file.getNode(), tokens);
            TokenizedSource tokenizedSource = new TokenizedSource(path, tokens);
            root.addTokenizedFile(tokenizedSource);
            visitor.setCurrentSourceFile(path);
            file.acceptChildren(new PsiRecursiveElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (visitor.process(element)) {
                        super.visitElement(element);
                        visitor.postProcess(element);
                    }
                }
            });
        }
        visitor.postProcess();
        return root;
    }

    protected void tokenize(final ASTNode node, List<TokenPosition> tokens) {
        ASTNode[] children = node.getChildren(TokenSet.andNot(TokenSet.ANY, TokenSet.WHITE_SPACE));
        if (children.length == 0) {
            PsiElement element = node.getPsi();
            if (element instanceof PsiComment) {
                CharBuffer comment = CharBuffer.wrap(element.getText());
                Matcher matcher = pattern.matcher(comment);

                while (matcher.find()) {
                    String token = matcher.group();
                    if (!token.equals("*")) {
                        int start = element.getTextOffset();
                        tokens.add(new TokenPosition(start + matcher.start(), start + matcher.end()));
                    }
                }
            } else if (!(element instanceof PsiWhiteSpace)) {
                tokens.add(new TokenPosition(element.getTextRange()));
            }
        } else
            Arrays.stream(children).forEachOrdered(n -> tokenize(n, tokens));
    }

    protected String getPattern() {
        return "\\S+";
    }

    @Override
    public FilePathFilter getAllowedFilesFilter() {
        return visitor.getAllowedFilesFilter();
    }
}

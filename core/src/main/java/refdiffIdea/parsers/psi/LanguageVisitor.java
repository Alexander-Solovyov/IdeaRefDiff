package refdiffIdea.parsers.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.core.cst.CstRoot;
import refdiffIdea.core.io.FilePathFilter;

public interface LanguageVisitor {
    void preProcess(@NotNull CstRoot root);
    void postProcess();
    @NotNull
    FilePathFilter getAllowedFilesFilter();

    void setCurrentSourceFile(@NotNull final String sourceFile);
    boolean process(@NotNull final PsiElement element);
    void postProcess(@NotNull final PsiElement element);
}

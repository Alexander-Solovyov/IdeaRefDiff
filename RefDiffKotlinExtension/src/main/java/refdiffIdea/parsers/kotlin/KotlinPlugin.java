package refdiffIdea.parsers.kotlin;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtElement;
import refdiffIdea.core.cst.CstRoot;
import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.parsers.LanguagePlugin;
import refdiffIdea.parsers.psi.LanguageVisitor;
import refdiffIdea.parsers.psi.PsiPlugin;

import java.util.Collections;

public class KotlinPlugin implements LanguageVisitor {
    private KotlinCstModel model = null;
    private KotlinPsiVisitor visitor = null;

    public static LanguagePlugin create(@NotNull Project project) {
        return new PsiPlugin(project, new KotlinPlugin());
    }

    @Override
    public void preProcess(@NotNull CstRoot root) {
        model = new KotlinCstModel(root);
    }

    @Override
    public void postProcess() {
        if (visitor != null)
            visitor.processContext();
        model.postProcess();
    }

    @Override
    public @NotNull FilePathFilter getAllowedFilesFilter() {
        return new FilePathFilter(Collections.singletonList(".kt"));
    }

    @Override
    public void setCurrentSourceFile(@NotNull String sourceFile) {
        if (visitor != null)
            visitor.processContext();
        visitor = new KotlinPsiVisitor(model, sourceFile);
    }

    @Override
    public boolean process(@NotNull PsiElement element) {
        if (element instanceof KtElement)
            ((KtElement) element).accept(visitor, null);
        return true;
    }

    @Override
    public void postProcess(@NotNull PsiElement element) {
        visitor.exit(element);
    }
}

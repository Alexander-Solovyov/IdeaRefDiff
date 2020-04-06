package refdiffIdea.parsers.java;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;

import org.jetbrains.annotations.NotNull;

import refdiffIdea.core.cst.*;
import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.parsers.LanguagePlugin;
import refdiffIdea.parsers.psi.LanguageVisitor;
import refdiffIdea.parsers.psi.PsiPlugin;

import java.util.*;

public class JavaPlugin implements LanguageVisitor {
    private JavaCstModel model = null;
    private JavaPsiVisitor visitor = null;

    public static LanguagePlugin create(@NotNull Project project) {
        return new PsiPlugin(project, new JavaPlugin());
    }

    @Override
    public void preProcess(CstRoot root) {
        model = new JavaCstModel(root);
    }

    @Override
    public void postProcess() {
        model.postProcess();
    }

    @Override
    public FilePathFilter getAllowedFilesFilter() {
        return new FilePathFilter(Collections.singletonList(".java"));
    }

    @Override
    public void setCurrentSourceFile(@NotNull String sourceFilePath) {
        visitor = new JavaPsiVisitor(model, sourceFilePath);
    }

    @Override
    public boolean process(PsiElement element) {
        element.accept(visitor);
        return !(element instanceof PsiAnonymousClass);
    }

    @Override
    public void postProcess(PsiElement element) {
        visitor.exit(element);
    }
}

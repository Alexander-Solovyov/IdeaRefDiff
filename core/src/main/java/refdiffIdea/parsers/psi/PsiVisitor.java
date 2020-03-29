package refdiffIdea.parsers.psi;

import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

public abstract class PsiVisitor extends PsiRecursiveElementVisitor {
    @Override
    public void visitElement(final PsiElement element) {
        if (process(element)) {
            super.visitElement(element);
            postProcess(element);
        }
    }

    protected boolean isSetter(@NotNull PsiMethod element) {
        String name = element.getName();
        if (!name.startsWith("set") && element.getParameterList().getParameters().length != 1) {
            return false;
        }
        PsiCodeBlock block = element.getBody();
        return block == null || block.getStatementCount() < 2;
    }

    protected boolean isGetter(@NotNull PsiMethod element) {
        String name = element.getName();
        if (!name.startsWith("get") && !name.startsWith("is") && !element.getParameterList().isEmpty()) {
            return false;
        }
        PsiCodeBlock block = element.getBody();
        return block == null || block.getStatementCount() < 2;
    }

    protected void postProcess(final PsiElement element) {}
    protected abstract boolean process(final PsiElement element);
}

package refdiffIdea.parsers.java;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.core.cst.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class JavaPsiVisitor extends JavaElementVisitor {
    private final JavaCstModel model;
    private String namespaceName = "";
    private final String sourceFilePath;
    private final LinkedList<HasChildrenNodes> containerStack = new LinkedList<>();

    JavaPsiVisitor(@NotNull JavaCstModel model, @NotNull String sourceFilePath) {
        this.model = model;
        this.sourceFilePath = sourceFilePath;
        containerStack.push(model.getRoot());
    }

    public void exit(PsiElement element) {
        if (element instanceof PsiClass) {
            containerStack.pop();
        }
    }

    @Override
    public void visitPackage(PsiPackage element) {
        namespaceName = element.getQualifiedName() + ".";
    }

    @Override
    public void visitAnonymousClass(PsiAnonymousClass element) {}

    @Override
    public void visitClass(PsiClass element) {
        int startPos = element.getTextRange().getStartOffset();
        int endPos = element.getTextRange().getEndOffset();
        String name = element.getName();

        CstNode node = model.createNode();
        node.setType(element.isEnum() ? "EnumDeclaration" :
                element.isInterface() ? "InterfaceDeclaration" : "ClassDeclaration");
        node.setLocation(Location.of(sourceFilePath, startPos, endPos, startPos, endPos, element.getContainingFile().getText()));
        node.setLocalName(name);
        node.setSimpleName(name);
        node.setNamespace(element.getParent() instanceof PsiFile ? namespaceName : "");
        if (element.isInterface()) {
            node.addStereotypes(Stereotype.ABSTRACT);
        }

        String uniqueName = element.getQualifiedName();
        if (uniqueName != null && !uniqueName.isEmpty()) {
            model.linkNodeToName(uniqueName, node);
        }

        if (element.isDeprecated()) {
            node.addStereotypes(Stereotype.DEPRECATED);
        }

        addSupertypesForPostprocessing(node, element.getSupers());

        containerStack.peek().addNode(node);
        containerStack.push(node);
    }

    @Override
    public void visitMethod(PsiMethod element) {
        CstNode node = model.createNode();
        node.setType(element.getClass().getSimpleName());
        node.addStereotypes(element.isConstructor() ? Stereotype.TYPE_CONSTRUCTOR : Stereotype.TYPE_MEMBER);
        if (element.isDeprecated()) {
            node.addStereotypes(Stereotype.DEPRECATED);
        }

        PsiCodeBlock body = element.getBody();
        int bodyStart;
        int bodyEnd;
        if (body == null) {
            node.addStereotypes(Stereotype.ABSTRACT);
            bodyEnd = bodyStart = element.getTextRange().getEndOffset();
        } else {
            bodyStart = body.getTextRange().getStartOffset() + 1;
            bodyEnd = body.getTextRange().getEndOffset() - 1;
        }
        node.setLocation(Location.of(sourceFilePath, element.getTextRange().getStartOffset(),
                element.getTextRange().getEndOffset(), bodyStart, bodyEnd, element.getContainingFile().getText()));
        String signature = calculateSignature(element);
        node.setLocalName(signature);
        node.setSimpleName(element.isConstructor() ? "new" : element.getName());

        if (element.getContainingClass() != null && element.getContainingClass().getQualifiedName() != null) {
            String uniqueName = element.getContainingClass().getQualifiedName() + "." + signature;
            model.linkNodeToName(uniqueName, node);
        }

        Arrays.stream(element.getParameterList().getParameters()).forEachOrdered(
                parameter -> node.getParameters().add(new Parameter(parameter.getName())));

        if (isGetter(element)) {
            node.addStereotypes(Stereotype.FIELD_ACCESSOR);
        } else if (isSetter(element)) {
            node.addStereotypes(Stereotype.FIELD_MUTATOR);
        }

        PsiCodeBlock block = element.getBody();
        if (block != null) {
            List<String> references = new ArrayList<>();
            block.acceptChildren(new PsiRecursiveElementVisitor() {
                @Override
                public void visitElement(PsiElement element) {
                    if (element instanceof PsiMethodCallExpression) {
                        PsiMethod method = ((PsiMethodCallExpression) element).resolveMethod();
                        if (method != null) {
                            PsiClass owner = method.getContainingClass();
                            if (owner != null && owner.getQualifiedName() != null) {
                                references.add(owner.getQualifiedName() + "." + calculateSignature(method));
                            }
                        }
                    }
                    super.visitElement(element);
                }
            });
            model.addReferences(node, references);
        }

        containerStack.peek().addNode(node);
    }

    private static String calculateSignature(PsiMethod element) {
        StringBuilder builder = new StringBuilder();
        builder.append(element.isConstructor() ? "new" : element.getName());
        builder.append('(');
        boolean firstParam = true;
        for (PsiParameter parameter : element.getParameterList().getParameters()) {
            if (!firstParam) {
                builder.append(", ");
            }
            firstParam = false;
            builder.append(parameter.getType().getPresentableText());
        }
        builder.append(')');
        return builder.toString();
    }

    private void addSupertypesForPostprocessing(@NotNull CstNode node, PsiClass[] supers) {
        if (supers.length == 0)
            return;

        List<String> supertypes = new ArrayList<>();
        for (PsiClass baseClass : supers) {
            PsiClass supertype = baseClass;
            while (supertype != null) {
                String className = baseClass.getQualifiedName();
                if (className != null && !className.isEmpty()) {
                    // Stop adding parent classes when we reach Object or Enum
                    PsiClass superClass = supertype.getSuperClass();
                    if (superClass != null) {
                        supertypes.add(className);
                    }
                    supertype = superClass;
                } else {
                    supertype = null;
                }
            }
        }
        model.addSupertypes(node, supertypes);
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
}

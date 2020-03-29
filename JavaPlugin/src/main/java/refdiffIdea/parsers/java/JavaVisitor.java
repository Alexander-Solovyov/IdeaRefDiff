package refdiffIdea.parsers.java;

import com.intellij.psi.*;

import org.jetbrains.annotations.NotNull;

import refdiffIdea.core.cst.*;
import refdiffIdea.parsers.psi.PsiVisitor;

import java.util.*;

public class JavaVisitor extends PsiVisitor {
    private String namespaceName = "";
    private String sourceFilePath;
    private static int nodeNum = 0;
    private Map<String, CstNode> nodesByUniqueName = new HashMap<>();
    private Map<CstNode, List<String>> postProcessReferences;
    private Map<CstNode, List<String>> postProcessSupertypes;

    private final LinkedList<HasChildrenNodes> containerStack = new LinkedList<>();

    JavaVisitor(@NotNull CstRoot root, @NotNull String sourceFilePath,
                @NotNull Map<String, CstNode> nodesByUniqueName,
                @NotNull Map<CstNode, List<String>> postProcessReferences,
                @NotNull Map<CstNode, List<String>> postProcessSupertypes) {
        containerStack.push(root);
        this.sourceFilePath = sourceFilePath;
        this.nodesByUniqueName = nodesByUniqueName;
        this.postProcessReferences = postProcessReferences;
        this.postProcessSupertypes = postProcessSupertypes;
    }

    static public void refreshCounter() { nodeNum = 0; }

    @Override
    protected boolean process(PsiElement element) {
        if (element instanceof PsiPackage) {
            namespaceName = ((PsiPackage) element).getQualifiedName() + ".";
        } else if (element instanceof PsiAnonymousClass) {
            return false;
        } else if (element instanceof PsiClass) {
            process((PsiClass) element);
        } else if (element instanceof PsiMethod) {
            process((PsiMethod) element);
        }
        return true;
    }

    @Override
    protected void postProcess(PsiElement element) {
        if (element instanceof PsiClass) {
           containerStack.pop();
        }
    }

    private void process(PsiClass element) {
        int startPos = element.getTextRange().getStartOffset();
        int endPos = element.getTextRange().getEndOffset();
        String name = element.getName();

        CstNode node = new CstNode(++nodeNum);
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
            nodesByUniqueName.put(uniqueName, node);
        }

        if (element.isDeprecated()) {
            node.addStereotypes(Stereotype.DEPRECATED);
        }

        addSupertypesForPostprocessing(node,element.getSupers());

        containerStack.peek().addNode(node);
        containerStack.push(node);
    }

    private void process(@NotNull PsiMethod element) {
        CstNode node = new CstNode(++nodeNum);
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
            nodesByUniqueName.put(uniqueName, node);
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
            postProcessReferences.put(node, references);
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

        List<String> supertypes = postProcessSupertypes.computeIfAbsent(node, k -> new ArrayList<>());
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
    }
}

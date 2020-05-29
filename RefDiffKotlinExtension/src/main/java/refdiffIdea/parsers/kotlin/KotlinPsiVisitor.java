package refdiffIdea.parsers.kotlin;

import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.*;
import refdiffIdea.core.cst.*;

import java.util.*;
import java.util.stream.Collectors;

public class KotlinPsiVisitor extends KtVisitorVoid {
    private final KotlinCstModel model;
    private String namespaceName = "";
    private final String sourceFilePath;
    private final LinkedList<HasChildrenNodes> containerStack = new LinkedList<>();
    private ContextNode contextNode = new ContextNode();


    KotlinPsiVisitor(@NotNull KotlinCstModel model, @NotNull String sourceFilePath) {
        this.model = model;
        this.sourceFilePath = sourceFilePath;
        containerStack.push(model.getRoot());
    }

    public void exit(PsiElement element) {
        if (element instanceof KtClass) {
            contextNode = contextNode.getParent();
            containerStack.pop();
        } else if (element instanceof KtFunction) {
            contextNode = contextNode.getParent();
        }
    }

    @Override
    public Void visitPackageDirective(@NotNull KtPackageDirective directive, Void data) {
        namespaceName = directive.getQualifiedName();
        return null;
    }

    @Override
    public void visitClass(@NotNull KtClass klass) {
        int startPos = klass.getTextRange().getStartOffset();
        int endPos = klass.getTextRange().getEndOffset();
        String name = klass.getName();

        CstNode node = model.createNode();
        node.setFile(klass.getContainingFile());
        node.setType(klass.isEnum() ? "EnumDeclaration" :
                klass.isInterface() ? "InterfaceDeclaration" : "ClassDeclaration");
        node.setLocation(Location.of(sourceFilePath, startPos, endPos, startPos, endPos, klass.getContainingFile().getText()));
        node.setLocalName(name);
        node.setSimpleName(name);
        node.setNamespace(klass.getParent() instanceof PsiFile ? namespaceName : "");
        if (klass.isInterface()) {
            node.addStereotypes(Stereotype.ABSTRACT);
        }

        FqName fqName = klass.getFqName();
        if (fqName != null) {
            model.linkNodeToName(fqName.asString(), node);
        }

        addSupertypesForPostprocessing(node, klass.getSuperTypeListEntries());
        containerStack.peek().addNode(node);
        containerStack.push(node);
        contextNode = contextNode.addNode(node, null);
    }

    @Override
    public void visitNamedFunction(@NotNull KtNamedFunction function) {
        visitFunction(function);
    }

    @Override
    public void visitPrimaryConstructor(@NotNull KtPrimaryConstructor constructor) {
        visitFunction(constructor);
    }

    @Override
    public void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor) {
        visitFunction(constructor);
    }

    public void visitFunction(@NotNull KtFunction function) {
        CstNode node = model.createNode();
        node.setFile(function.getContainingFile());
        node.setType(function.getClass().getSimpleName());

        int bodyStart = 0;
        int bodyEnd = 0;
        KtExpression body = function.getBodyExpression();
        if (body == null) {
            node.addStereotypes(Stereotype.ABSTRACT);
            bodyEnd = bodyStart = function.getTextRange().getEndOffset();
        } else if (!(body  instanceof KtBlockExpression)) {
            bodyStart = body.getTextRange().getStartOffset();
            bodyEnd = body.getTextRange().getEndOffset();
        } else {
            bodyStart = body.getTextRange().getStartOffset() + 1;
            bodyEnd = body.getTextRange().getEndOffset() - 1;
        }

        node.setLocation(Location.of(sourceFilePath, function.getTextRange().getStartOffset(),
                function.getTextRange().getEndOffset(), bodyStart, bodyEnd, function.getContainingFile().getText()));
        String signature = calculateSignature(function);
        node.setLocalName(signature);
        String name = function.getName();
        node.setSimpleName(name == null ? "new" : name);

        FqName fqName = function.getFqName();
        if (fqName != null && !fqName.isRoot()) {
            String uniqueName = fqName.parent().asString() + "." + signature;
            model.linkNodeToName(uniqueName, node);
        }

        function.getValueParameters().stream().forEachOrdered(
                ktParameter -> {
                    String parameter = ktParameter.getName();
                    if (parameter != null)
                        node.getParameters().add(new Parameter(ktParameter.getName()));
                });

        contextNode = contextNode.addNode(node, function);
        containerStack.peek().addNode(node);
    }

    private String calculateSignature(@NotNull KtFunction function) {
        StringBuilder builder = new StringBuilder();
        String name = function.getName();
        builder.append(name == null ? "new" : name);
        builder.append('(');
        boolean firstParam = true;
        for (KtTypeParameter parameter : function.getTypeParameters()) {
            if (!firstParam) {
                builder.append(", ");
            }
            firstParam = false;
            builder.append(parameter.getName());
        }
        builder.append(')');
        return builder.toString();
    }

    private void addSupertypesForPostprocessing(@NotNull CstNode node, List<KtSuperTypeListEntry> entries) {
        if (entries.isEmpty())
            return;

        List<String> supertypes = entries.stream().map(KtSuperTypeListEntry::getTypeAsUserType).filter(Objects::nonNull).
                map(KtUserType::getReferencedName).filter(Objects::nonNull).collect(Collectors.toList());
        if (!supertypes.isEmpty())
            model.addSupertypes(node,supertypes);
    }

    public void processContext() {
        contextNode.forEachFunction(node -> {
            KtFunction function = ((KtFunction)node.getElement());
            KtExpression body = function.getBodyExpression();
            if (body == null)
                return;

            Set<CstNode> references = new HashSet<>();
            function.getBodyExpression().accept(new KtTreeVisitorVoid() {
                @Override
                public void visitKtElement(@NotNull KtElement element) {
                    if (element instanceof KtCallExpression) {
                        final KtCallExpression callExpression = (KtCallExpression) element;
                        KtExpression callee = callExpression.getCalleeExpression();
                        if (callee != null) {
                            ContextNode correspondingNode = node.searchParent(function ->
                                    function != null && function.getName().equals(callee.getText()) &&
                                    function.getValueParameters().size() == callExpression.getValueArguments().size());
                            if (correspondingNode != null)
                                references.add(correspondingNode.getNode());
                        }
                    }
                    else
                        super.visitKtElement(element);
                }
            });
            model.addReference(node.getNode(), references);
        });
    }
}

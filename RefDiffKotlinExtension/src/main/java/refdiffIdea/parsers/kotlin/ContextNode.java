package refdiffIdea.parsers.kotlin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtFunction;
import refdiffIdea.core.cst.CstNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ContextNode {
    private CstNode cstNode = null;
    private KtFunction function = null;
    private ContextNode parentNode = null;
    private List<ContextNode> children = new ArrayList<>();

    public ContextNode() {}

    private ContextNode(CstNode node, KtFunction function) {
        cstNode = node;
        this.function = function;
    }

    private void addParent(@NotNull ContextNode node) {
        parentNode = node;
    }

    @Nullable
    public ContextNode getParent() { return parentNode; }

    public ContextNode addNode(@NotNull CstNode node, KtFunction function) {
        ContextNode newNode = new ContextNode(node, function);
        children.add(newNode);
        newNode.addParent(this);
        return newNode;
    }

    public void forEachFunction(Consumer<ContextNode> action) {
        for (ContextNode child : children) {
            child.forEachFunction(action);
        }

        if (function != null) {
            action.accept(this);
        }
    }

    public CstNode getNode() {
        return cstNode;
    }

    public KtFunction getElement() {
        return function;
    }

    @Nullable
    public ContextNode searchParent(@NotNull Predicate<KtFunction> predicate) {
        if (predicate.test(function))
            return this;

        ContextNode parent = getParent();
        while (parent != null && !predicate.test(parent.function)) {
            for (ContextNode child: parent.children) {
                if (predicate.test(child.function))
                    return child;
            }
            parent = parent.getParent();
        }
        return parent;
    }
}

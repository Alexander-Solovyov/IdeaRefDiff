package refdiffIdea.parsers.kotlin;

import org.jetbrains.annotations.NotNull;
import refdiffIdea.core.cst.CstNode;
import refdiffIdea.core.cst.CstNodeRelationship;
import refdiffIdea.core.cst.CstNodeRelationshipType;
import refdiffIdea.core.cst.CstRoot;

import java.util.*;

public class KotlinCstModel {
    private final CstRoot root;
    private int nodeNum = 0;
    private Map<String, CstNode> nodesByUniqueName           = new HashMap<>();
    private Map<CstNode, List<String>> postProcessSupertypes = new HashMap<>();

    public KotlinCstModel(@NotNull CstRoot root) {
        this.root = root;
    }

    public CstNode createNode() {
        return new CstNode(++nodeNum);
    }

    public CstRoot getRoot() {
        return root;
    }

    public void linkNodeToName(String name, CstNode node) {
        nodesByUniqueName.put(name, node);
    }

    public void addSupertypes(CstNode node, List<String> newSupertypes) {
        postProcessSupertypes.computeIfAbsent(node, k -> new ArrayList<>()).addAll(newSupertypes);
    }

    public void addReference(CstNode node, Set<CstNode> references) {
        references.forEach(referenced ->
                root.getRelationships().add(new CstNodeRelationship(
                        CstNodeRelationshipType.USE, node.getId(), referenced.getId())));
    }

    public void postProcess() {
        postProcess(postProcessSupertypes, CstNodeRelationshipType.SUBTYPE);
    }

    private void postProcess(Map<CstNode, List<String>> relations, CstNodeRelationshipType type) {
        for (Map.Entry<CstNode, List<String>> entry : relations.entrySet()) {
            CstNode node = entry.getKey();
            for (String name : entry.getValue()) {
                CstNode relation = nodesByUniqueName.get(name);
                if (relation != null) {
                    root.getRelationships().add(new CstNodeRelationship(type,node.getId(),relation.getId()));
                }
            }
        }
    }
}

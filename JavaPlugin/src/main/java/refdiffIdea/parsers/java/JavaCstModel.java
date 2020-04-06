package refdiffIdea.parsers.java;

import org.jetbrains.annotations.NotNull;

import refdiffIdea.core.cst.CstNode;
import refdiffIdea.core.cst.CstNodeRelationship;
import refdiffIdea.core.cst.CstNodeRelationshipType;
import refdiffIdea.core.cst.CstRoot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaCstModel {
    private final CstRoot root;
    private int nodeNum = 0;
    private Map<String, CstNode> nodesByUniqueName           = new HashMap<>();
    private Map<CstNode, List<String>> postProcessReferences = new HashMap<>();
    private Map<CstNode, List<String>> postProcessSupertypes = new HashMap<>();

    public JavaCstModel(@NotNull CstRoot root) {
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

    public void addReferences(CstNode node, List<String> newReferences) {
        postProcessReferences.put(node, newReferences);
    }

    public void postProcess() {
        // methods
        postProcess(postProcessReferences, CstNodeRelationshipType.USE);
        // superclasses
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

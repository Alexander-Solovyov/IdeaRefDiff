package refdiffIdea.parsers.java;

import com.intellij.openapi.project.Project;

import refdiffIdea.core.cst.CstNode;
import refdiffIdea.core.cst.CstNodeRelationship;
import refdiffIdea.core.cst.CstNodeRelationshipType;
import refdiffIdea.core.cst.CstRoot;
import refdiffIdea.core.io.FilePathFilter;
import refdiffIdea.core.io.SourceFile;
import refdiffIdea.parsers.psi.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaPlugin extends PsiPlugin {
    CstRoot root;
    private Map<String, CstNode> nodesByUniqueName;
    private Map<CstNode, List<String>> postProcessReferences;
    private Map<CstNode, List<String>> postProcessSupertypes;

    public JavaPlugin(Project project) {
        super(project);
    }

    @Override
    protected PsiVisitor getVisitor(SourceFile sourceFile) {
        return new JavaVisitor(root, sourceFile.getPath(),
                nodesByUniqueName, postProcessReferences, postProcessSupertypes);
    }

    @Override
    protected void preProcess(CstRoot root) {
        JavaVisitor.refreshCounter();
        this.root = root;
        nodesByUniqueName = new HashMap<>();
        postProcessReferences = new HashMap<>();
        postProcessSupertypes = new HashMap<>();
    }

    @Override
    protected void postProcess() {
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

    @Override
    public FilePathFilter getAllowedFilesFilter() {
        return new FilePathFilter(Collections.singletonList(".java"));
    }
}

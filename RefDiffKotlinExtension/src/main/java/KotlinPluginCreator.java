import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.parsers.LanguagePlugin;

import refdiffIdea.parsers.kotlin.KotlinPlugin;

public class KotlinPluginCreator implements LanguagePluginCreator{
    @Override
    public @NotNull LanguagePlugin create(@NotNull Project project) {
        return KotlinPlugin.create(project);
    }
}

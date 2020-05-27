import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.parsers.LanguagePlugin;
import refdiffIdea.parsers.java.JavaPlugin;

public class JavaPluginCreator implements LanguagePluginCreator {
    @Override
    public @NotNull LanguagePlugin create(@NotNull Project project) {
        return JavaPlugin.create(project);
    }
}

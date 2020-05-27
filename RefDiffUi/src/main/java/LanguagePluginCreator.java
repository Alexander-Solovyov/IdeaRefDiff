import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import refdiffIdea.parsers.LanguagePlugin;

public interface LanguagePluginCreator {
    ExtensionPointName<LanguagePluginCreator> Extensions = ExtensionPointName.create("io.solovyov.alexander.RefDiffUi.languagePluginCreator");

    @NotNull LanguagePlugin create(@NotNull Project project);
}

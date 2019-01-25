package org.javacomp.completion;

import com.google.common.collect.ImmutableMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import org.javacomp.model.ClassEntity;
import org.javacomp.protocol.textdocument.CompletionItem.ResolveAction;
import org.javacomp.protocol.textdocument.CompletionItem.ResolveActionParams;
import org.javacomp.protocol.textdocument.CompletionItem.ResolveAddImportTextEditsParams;
import org.javacomp.typesolver.EntityShadowingListBuilder.ForImportEntity;

/** A candidate with import class edit actions. */
class ClassForImportCandidate extends EntityBasedCompletionCandidate {
  private final Path filePath;

  ClassForImportCandidate(ClassEntity classEntity, String filePath) {
    super(new ForImportEntity(classEntity));
    this.filePath = Paths.get(filePath);
  }

  @Override
  public String getName() {
    return getEntity().getSimpleName();
  }

  @Override
  public Kind getKind() {
    return Kind.CLASS;
  }

  @Override
  public Optional<String> getDetail() {
    return Optional.of(getEntity().getQualifiedName());
  }

  @Override
  public SortCategory getSortCategory() {
    return SortCategory.TO_IMPORT;
  }

  @Override
  public Map<ResolveAction, ResolveActionParams> getResolveActions() {
    ImmutableMap.Builder<ResolveAction, ResolveActionParams> builder = new ImmutableMap.Builder<>();
    ResolveAddImportTextEditsParams params = new ResolveAddImportTextEditsParams();
    params.uri = filePath.toUri();
    params.classFullName = getEntity().getQualifiedName();
    builder.put(ResolveAction.ADD_IMPORT_TEXT_EDIT, params);
    builder.putAll(super.getResolveActions());
    return builder.build();
  }
}

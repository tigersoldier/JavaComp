package org.javacomp.server.handler;

import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.VariableEntity;
import org.javacomp.project.Project;
import org.javacomp.protocol.MarkupContent;
import org.javacomp.protocol.SignatureHelp;
import org.javacomp.protocol.TextDocumentPositionParams;
import org.javacomp.reference.MethodSignatures;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/signatureHelp" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#signature-help-request
 */
public class SignatureHelpTextDocumentHandler extends RequestHandler<TextDocumentPositionParams> {
  private final Server server;

  public SignatureHelpTextDocumentHandler(Server server) {
    super("textDocument/signatureHelp", TextDocumentPositionParams.class);
    this.server = server;
  }

  @Override
  public SignatureHelp handleRequest(Request<TextDocumentPositionParams> request) throws Exception {
    TextDocumentPositionParams params = request.getParams();
    Project project = server.getProject();
    MethodSignatures methodSignatures =
        project.findMethodSignatures(
            Paths.get(params.textDocument.uri),
            params.position.getLine(),
            params.position.getCharacter());

    SignatureHelp ret = new SignatureHelp();
    ret.activeSignature = 0;
    ret.activeParameter = methodSignatures.getActiveParameter();
    ret.signatures =
        methodSignatures
            .getMethods()
            .stream()
            .map((method) -> convertMethod(method, methodSignatures.getActiveParameter()))
            .collect(ImmutableList.toImmutableList());
    return ret;
  }

  private SignatureHelp.SignatureInformation convertMethod(
      MethodEntity method, int activeParameter) {
    SignatureHelp.SignatureInformation signature = new SignatureHelp.SignatureInformation();
    signature.parameters =
        method
            .getParameters()
            .stream()
            .map((param) -> convertParameter(param))
            .collect(ImmutableList.toImmutableList());

    StringBuilder sb = new StringBuilder();
    if (!method.getTypeParameters().isEmpty()) {
      sb.append("<");
      sb.append(
          method
              .getTypeParameters()
              .stream()
              .map(t -> t.toDisplayString())
              .collect(Collectors.joining(", ")));
    }
    sb.append(method.getReturnType().toDisplayString());
    sb.append(" ");
    sb.append(method.getSimpleName());
    sb.append("(");
    for (int i = 0; i < method.getParameters().size(); i++) {
      if (i > 0) {
        sb.append(", ");
      }
      VariableEntity parameter = method.getParameters().get(i);
      sb.append(signature.parameters.get(i).label);
    }
    sb.append(")");
    signature.label = sb.toString();
    if (method.getJavadoc().isPresent()) {
      signature.documentation = MarkupContent.markdown(method.getJavadoc().get());
    }

    return signature;
  }

  private SignatureHelp.ParameterInformation convertParameter(VariableEntity parameter) {
    SignatureHelp.ParameterInformation paramInfo = new SignatureHelp.ParameterInformation();
    paramInfo.label = parameter.getType().toDisplayString() + " " + parameter.getSimpleName();
    return paramInfo;
  }
}

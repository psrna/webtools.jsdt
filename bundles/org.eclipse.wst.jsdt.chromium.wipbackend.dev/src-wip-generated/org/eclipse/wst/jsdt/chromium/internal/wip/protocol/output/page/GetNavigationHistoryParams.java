// Generated source.
// Generator: org.eclipse.wst.jsdt.chromium.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.eclipse.wst.jsdt.chromium.internal.wip.protocol.output.page;

/**
Returns navigation history for the current page.
 */
public class GetNavigationHistoryParams extends org.eclipse.wst.jsdt.chromium.internal.wip.protocol.output.WipParamsWithResponse<org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.page.GetNavigationHistoryData> {
  public GetNavigationHistoryParams() {
  }

  public static final String METHOD_NAME = org.eclipse.wst.jsdt.chromium.internal.wip.protocol.BasicConstants.Domain.PAGE + ".getNavigationHistory";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

  @Override public org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.page.GetNavigationHistoryData parseResponse(org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.WipCommandResponse.Data data, org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.WipGeneratedParserRoot parser) throws org.eclipse.wst.jsdt.chromium.internal.protocolparser.JsonProtocolParseException {
    return parser.parsePageGetNavigationHistoryData(data.getUnderlyingObject());
  }

}

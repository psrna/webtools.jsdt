// Generated source.
// Generator: org.eclipse.wst.jsdt.chromium.internal.wip.tools.protocolgenerator.Generator
// Origin: http://svn.webkit.org/repository/webkit/trunk/Source/WebCore/inspector/Inspector.json@96703

package org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console;

/**
 Issued when new console message is added.
 */
@org.eclipse.wst.jsdt.chromium.internal.protocolparser.JsonType
public interface MessageAddedEventData {
  /**
   Console message that has been added.
   */
  org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console.ConsoleMessageValue message();

  public static final org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.WipEventType<org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console.MessageAddedEventData> TYPE
      = new org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.WipEventType<org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console.MessageAddedEventData>("Console.messageAdded", org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console.MessageAddedEventData.class) {
    @Override public org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.console.MessageAddedEventData parse(org.eclipse.wst.jsdt.chromium.internal.wip.protocol.input.WipGeneratedParserRoot parser, org.json.simple.JSONObject obj) throws org.eclipse.wst.jsdt.chromium.internal.protocolparser.JsonProtocolParseException {
      return parser.parseConsoleMessageAddedEventData(obj);
    }
  };
}

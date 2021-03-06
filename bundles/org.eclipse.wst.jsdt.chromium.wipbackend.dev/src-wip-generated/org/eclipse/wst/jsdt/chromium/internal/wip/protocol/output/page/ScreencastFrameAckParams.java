// Generated source.
// Generator: org.eclipse.wst.jsdt.chromium.internal.wip.tools.protocolgenerator.Generator
// Origin: http://src.chromium.org/blink/trunk/Source/devtools/protocol.json@<unknown>

package org.eclipse.wst.jsdt.chromium.internal.wip.protocol.output.page;

/**
Acknowledges that a screencast frame has been received by the frontend.
 */
public class ScreencastFrameAckParams extends org.eclipse.wst.jsdt.chromium.internal.wip.protocol.output.WipParams {
  /**
   @param frameNumber Frame number.
   */
  public ScreencastFrameAckParams(long frameNumber) {
    this.put("frameNumber", frameNumber);
  }

  public static final String METHOD_NAME = org.eclipse.wst.jsdt.chromium.internal.wip.protocol.BasicConstants.Domain.PAGE + ".screencastFrameAck";

  @Override protected String getRequestName() {
    return METHOD_NAME;
  }

}
